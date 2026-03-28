package org.dromara.docman.application.service;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.application.CommandApplicationService;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.file.FileUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.docman.application.port.out.DocumentStoragePort;
import org.dromara.docman.domain.bo.DocDocumentRecordBo;
import org.dromara.docman.domain.entity.DocDocumentRecord;
import org.dromara.docman.domain.vo.DocDocumentRecordVo;
import org.dromara.docman.service.IDocDocumentRecordService;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocDocumentApplicationService implements CommandApplicationService {

    private final IDocDocumentRecordService documentRecordService;
    private final DocumentStoragePort documentStoragePort;

    public TableDataInfo<DocDocumentRecordVo> list(Long projectId, PageQuery pageQuery) {
        return documentRecordService.queryPageList(projectId, pageQuery);
    }

    public DocDocumentRecordVo getById(Long id) {
        return documentRecordService.queryById(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public void upload(DocDocumentRecordBo bo) {
        documentRecordService.recordUpload(bo);
    }

    public void download(Long id, HttpServletResponse response) {
        DocDocumentRecord record = documentRecordService.queryEntityById(id);
        byte[] content = loadDocumentContentInternal(record);
        FileUtils.setAttachmentResponseHeader(response, resolveFileName(record));
        response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE + "; charset=UTF-8");
        response.setContentLengthLong(content.length);
        try {
            response.getOutputStream().write(content);
            response.getOutputStream().flush();
        } catch (IOException e) {
            throw new ServiceException("写出下载文件失败");
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        documentRecordService.markObsoleteById(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public void markObsolete(Long projectId) {
        documentRecordService.markObsoleteByProjectId(projectId);
    }

    public byte[] loadDocumentContent(Long id) {
        return loadDocumentContentInternal(documentRecordService.queryEntityById(id));
    }

    public byte[] loadDocumentContent(DocDocumentRecord record) {
        return loadDocumentContentInternal(record);
    }

    public String resolveFileName(Long id) {
        return resolveFileName(documentRecordService.queryEntityById(id));
    }

    public String resolveFileName(DocDocumentRecord record) {
        return resolveFileNameInternal(record);
    }

    public String resolveContentType(DocDocumentRecord record) {
        return MediaTypeFactory.getMediaType(resolveFileNameInternal(record))
            .map(MediaType::toString)
            .orElse(MediaType.APPLICATION_OCTET_STREAM_VALUE);
    }

    private byte[] loadDocumentContentInternal(DocDocumentRecord record) {
        String nasPath = record.getNasPath();
        if (nasPath == null || nasPath.isBlank()) {
            throw new ServiceException("文档存储路径为空");
        }
        try {
            return documentStoragePort.load(nasPath);
        } catch (Exception e) {
            log.warn("OSS读取失败，回退到本地存储路径: {}", nasPath, e);
            Path localPath = resolveLocalPath(nasPath);
            try {
                return Files.readAllBytes(localPath);
            } catch (IOException ioException) {
                throw new ServiceException("读取文档文件失败: " + localPath);
            }
        }
    }

    private Path resolveLocalPath(String nasPath) {
        Path localRoot = getLocalRoot();
        Path sourcePath = Paths.get(nasPath).normalize();
        Path localPath;
        if (sourcePath.isAbsolute()) {
            localPath = sourcePath.toAbsolutePath().normalize();
        } else {
            String relative = nasPath.startsWith("/") ? nasPath.substring(1) : nasPath;
            if (relative.isBlank()) {
                throw new ServiceException("文档存储路径为空");
            }
            localPath = localRoot.resolve(relative.replace("/", File.separator)).normalize();
        }
        if (!localPath.startsWith(localRoot)) {
            throw new ServiceException("非法文档存储路径: " + nasPath);
        }
        if (!Files.exists(localPath)) {
            throw new ServiceException("文档文件不存在: " + localPath);
        }
        return localPath;
    }

    private Path getLocalRoot() {
        String localRootProp = System.getProperty(
            "docman.upload.localRoot",
            System.getProperty("java.io.tmpdir") + File.separator + "docman-upload"
        );
        return Paths.get(localRootProp).toAbsolutePath().normalize();
    }

    private String resolveFileNameInternal(DocDocumentRecord record) {
        String fileName = record.getFileName();
        if (fileName != null && !fileName.isBlank()) {
            return fileName;
        }
        String nasPath = record.getNasPath();
        if (nasPath == null || nasPath.isBlank()) {
            return "document.bin";
        }
        int separatorIndex = Math.max(nasPath.lastIndexOf('/'), nasPath.lastIndexOf('\\'));
        String resolved = separatorIndex >= 0 ? nasPath.substring(separatorIndex + 1) : nasPath;
        return resolved.isBlank() ? "document.bin" : resolved;
    }
}
