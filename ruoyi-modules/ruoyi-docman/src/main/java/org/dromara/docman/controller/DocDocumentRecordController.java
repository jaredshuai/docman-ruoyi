package org.dromara.docman.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.domain.R;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.web.core.BaseController;
import org.dromara.docman.application.port.out.DocumentStoragePort;
import org.dromara.docman.application.service.DocDocumentApplicationService;
import org.dromara.docman.application.service.DocDocumentQueryApplicationService;
import org.dromara.docman.application.service.DocDocumentViewerApplicationService;
import org.dromara.docman.application.service.DocProjectQueryApplicationService;
import org.dromara.docman.domain.bo.DocDocumentRecordBo;
import org.dromara.docman.domain.enums.DocDocumentSourceType;
import org.dromara.docman.domain.vo.DocProjectVo;
import org.dromara.docman.domain.vo.DocDocumentRecordVo;
import org.dromara.docman.domain.vo.DocViewerTicketVo;
import org.dromara.docman.domain.vo.DocViewerUrlVo;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/docman/document")
public class DocDocumentRecordController extends BaseController {

    private final DocDocumentApplicationService documentApplicationService;
    private final DocDocumentQueryApplicationService documentQueryApplicationService;
    private final DocDocumentViewerApplicationService documentViewerApplicationService;
    private final DocProjectQueryApplicationService projectQueryApplicationService;
    private final DocumentStoragePort documentStoragePort;

    @SaCheckPermission("docman:document:list")
    @GetMapping("/list")
    public TableDataInfo<DocDocumentRecordVo> list(@RequestParam Long projectId, PageQuery pageQuery) {
        return documentQueryApplicationService.list(projectId, pageQuery);
    }

    @SaCheckPermission("docman:document:query")
    @GetMapping("/{id}")
    public R<DocDocumentRecordVo> getInfo(@PathVariable Long id) {
        return R.ok(documentQueryApplicationService.getById(id));
    }

    @SaCheckPermission("docman:document:download")
    @GetMapping("/{id}/download")
    public void download(@PathVariable Long id, HttpServletResponse response) {
        documentApplicationService.download(id, response);
    }

    @SaCheckPermission("docman:document:query")
    @PostMapping("/{id}/viewer-ticket")
    public R<DocViewerTicketVo> createViewerTicket(@PathVariable Long id) {
        return R.ok(documentViewerApplicationService.createViewerTicket(id));
    }

    @SaCheckPermission("docman:document:query")
    @GetMapping("/{id}/viewer-url")
    public R<DocViewerUrlVo> getViewerUrl(@PathVariable Long id) {
        return R.ok(documentViewerApplicationService.getViewerUrl(id));
    }

    @SaCheckPermission("docman:document:query")
    @GetMapping("/viewer/content/{ticket}")
    public void viewerContent(@PathVariable String ticket, HttpServletResponse response) {
        DocDocumentViewerApplicationService.ViewerContentPayload payload = documentViewerApplicationService.loadViewerContent(ticket);
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "inline; filename*=UTF-8''" + encodeFileName(payload.fileName()));
        response.setContentType(payload.contentType());
        response.setContentLengthLong(payload.content().length);
        try {
            response.getOutputStream().write(payload.content());
            response.getOutputStream().flush();
        } catch (IOException e) {
            throw new ServiceException("写出预览文件失败");
        }
    }

    @SaCheckPermission("docman:document:upload")
    @Log(title = "文档上传", businessType = BusinessType.INSERT)
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public R<Void> upload(@RequestPart("file") MultipartFile file, @RequestParam("projectId") Long projectId) {
        if (file == null || file.isEmpty()) {
            throw new ServiceException("上传文件不能为空");
        }

        long maxSize = 100L * 1024 * 1024;
        if (file.getSize() > maxSize) {
            throw new ServiceException("上传文件不能超过 100MB");
        }
        String originalFilename = file.getOriginalFilename();
        String ext = (originalFilename != null && originalFilename.contains("."))
            ? originalFilename.substring(originalFilename.lastIndexOf('.')).toLowerCase()
            : "";
        Set<String> allowedExt = Set.of(".pdf", ".doc", ".docx", ".xls", ".xlsx",
            ".ppt", ".pptx", ".txt", ".png", ".jpg", ".jpeg", ".zip", ".rar");
        if (!allowedExt.contains(ext)) {
            throw new ServiceException("不支持的文件类型: " + ext);
        }

        String fileName = sanitizeFileName(originalFilename);
        DocProjectVo project = projectQueryApplicationService.getById(projectId);
        if (project == null) {
            throw new ServiceException("项目不存在");
        }
        String projectBasePath = project.getNasBasePath();
        if (projectBasePath == null || projectBasePath.isBlank()) {
            projectBasePath = "/docman/project/" + projectId;
        }
        if (projectBasePath.endsWith("/")) {
            projectBasePath = projectBasePath.substring(0, projectBasePath.length() - 1);
        }

        String uploadsDir = projectBasePath + "/uploads";
        String nasPath = uploadsDir + "/" + UUID.randomUUID() + "-" + fileName;
        String contentType = (file.getContentType() == null || file.getContentType().isBlank())
            ? MediaType.APPLICATION_OCTET_STREAM_VALUE
            : file.getContentType();

        byte[] content = readBytes(file);
        Long ossId = null;
        try {
            boolean dirReady = documentStoragePort.ensureDirectory(uploadsDir);
            if (!dirReady) {
                log.warn("存储目录创建失败，将使用本地降级存储: {}", uploadsDir);
            }
            DocumentStoragePort.StoredDocument stored = documentStoragePort.store(nasPath, content, fileName, contentType);
            fileName = stored.fileName();
            nasPath = stored.path();
            ossId = stored.storageRecordId();
        } catch (Exception e) {
            // OSS/S3 未集成或不可用时，落地到本地路径（NAS-like）。
            writeLocal(nasPath, content);
        }

        DocDocumentRecordBo bo = new DocDocumentRecordBo();
        bo.setProjectId(projectId);
        bo.setSourceType(DocDocumentSourceType.UPLOAD.getCode());
        bo.setFileName(fileName);
        bo.setNasPath(nasPath);
        bo.setOssId(ossId);
        documentApplicationService.upload(bo);
        return R.ok();
    }

    @SaCheckPermission("docman:document:delete")
    @Log(title = "文档删除", businessType = BusinessType.DELETE)
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        documentApplicationService.delete(id);
        return R.ok();
    }

    private static byte[] readBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException e) {
            throw new ServiceException("读取上传文件失败");
        }
    }

    private static String sanitizeFileName(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            return "upload.bin";
        }
        String cleaned = Paths.get(originalFilename).getFileName().toString();
        if (cleaned.isBlank()) {
            return "upload.bin";
        }
        return cleaned;
    }

    private static void writeLocal(String nasPath, byte[] content) {
        String localRootProp = System.getProperty(
            "docman.upload.localRoot",
            System.getProperty("java.io.tmpdir") + File.separator + "docman-upload"
        );
        Path localRoot = Paths.get(localRootProp).toAbsolutePath().normalize();
        String relative = (nasPath != null && nasPath.startsWith("/")) ? nasPath.substring(1) : nasPath;
        if (relative == null || relative.isBlank()) {
            throw new ServiceException("存储路径为空");
        }
        Path localPath = localRoot.resolve(relative.replace("/", File.separator)).normalize();
        if (!localPath.startsWith(localRoot)) {
            throw new ServiceException("非法存储路径: " + nasPath);
        }
        try {
            Path parent = localPath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.write(localPath, content, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            throw new ServiceException("本地存储文件失败: " + localPath);
        }
    }

    private static String encodeFileName(String fileName) {
        return java.net.URLEncoder.encode(fileName, java.nio.charset.StandardCharsets.UTF_8)
            .replace("+", "%20");
    }
}
