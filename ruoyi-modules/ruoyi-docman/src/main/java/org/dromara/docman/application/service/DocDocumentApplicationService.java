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

    /**
     * 分页查询项目文档记录。
     *
     * @param projectId 项目ID
     * @param pageQuery 分页参数
     * @return 文档分页结果
     */
    public TableDataInfo<DocDocumentRecordVo> list(Long projectId, PageQuery pageQuery) {
        return documentRecordService.queryPageList(projectId, pageQuery);
    }

    /**
     * 查询单个文档详情。
     *
     * @param id 文档记录ID
     * @return 文档详情
     */
    public DocDocumentRecordVo getById(Long id) {
        return documentRecordService.queryById(id);
    }

    /**
     * 登记上传文档。
     *
     * @param bo 文档上传参数
     */
    @Transactional(rollbackFor = Exception.class)
    public void upload(DocDocumentRecordBo bo) {
        documentRecordService.recordUpload(bo);
    }

    /**
     * 下载指定文档内容。
     *
     * @param id       文档记录ID
     * @param response HTTP响应
     */
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

    /**
     * 将指定文档标记为失效。
     *
     * @param id 文档记录ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        documentRecordService.markObsoleteById(id);
    }

    /**
     * 将项目下所有文档标记为失效。
     *
     * @param projectId 项目ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void markObsolete(Long projectId) {
        documentRecordService.markObsoleteByProjectId(projectId);
    }

    /**
     * 按文档ID加载内容。
     *
     * @param id 文档记录ID
     * @return 文档二进制内容
     */
    public byte[] loadDocumentContent(Long id) {
        return loadDocumentContentInternal(documentRecordService.queryEntityById(id));
    }

    /**
     * 按文档实体加载内容。
     *
     * @param record 文档实体
     * @return 文档二进制内容
     */
    public byte[] loadDocumentContent(DocDocumentRecord record) {
        return loadDocumentContentInternal(record);
    }

    /**
     * 按文档ID解析下载文件名。
     *
     * @param id 文档记录ID
     * @return 文件名
     */
    public String resolveFileName(Long id) {
        return resolveFileName(documentRecordService.queryEntityById(id));
    }

    /**
     * 按文档实体解析下载文件名。
     *
     * @param record 文档实体
     * @return 文件名
     */
    public String resolveFileName(DocDocumentRecord record) {
        return resolveFileNameInternal(record);
    }

    /**
     * 根据文件名推断文档内容类型。
     *
     * @param record 文档实体
     * @return MIME类型
     */
    public String resolveContentType(DocDocumentRecord record) {
        return MediaTypeFactory.getMediaType(resolveFileNameInternal(record))
            .map(MediaType::toString)
            .orElse(MediaType.APPLICATION_OCTET_STREAM_VALUE);
    }

    private byte[] loadDocumentContentInternal(DocDocumentRecord record) {
        String nasPath = record.getNasPath();
        if (nasPath == null || nasPath.isBlank()) {
            throw new ServiceException("文档内容不可用");
        }
        try {
            return documentStoragePort.load(nasPath);
        } catch (Exception e) {
            log.warn("OSS读取失败，回退到本地存储", e);
            Path localPath = resolveLocalPath(nasPath);
            try {
                return Files.readAllBytes(localPath);
            } catch (IOException ioException) {
                throw new ServiceException("文档内容读取失败");
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
                throw new ServiceException("文档内容不可用");
            }
            localPath = localRoot.resolve(relative.replace("/", File.separator)).normalize();
        }
        if (!localPath.startsWith(localRoot)) {
            throw new ServiceException("文档内容读取失败");
        }
        if (!Files.exists(localPath)) {
            throw new ServiceException("文档内容读取失败");
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
