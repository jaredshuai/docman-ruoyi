package org.dromara.docman.infrastructure.storage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.port.InfrastructureAdapter;
import org.dromara.common.json.utils.JsonUtils;
import org.dromara.common.oss.core.OssClient;
import org.dromara.common.oss.entity.UploadResult;
import org.dromara.common.oss.factory.OssFactory;
import org.dromara.docman.application.port.out.DocumentStoragePort;
import org.dromara.system.domain.SysOss;
import org.dromara.system.domain.SysOssExt;
import org.dromara.system.mapper.SysOssMapper;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

@Slf4j
@RequiredArgsConstructor
@InfrastructureAdapter("S3/OSS 文档存储")
public class OssDocumentStorageAdapter implements DocumentStoragePort {

    private static final String LOCAL_ONLY_PROPERTY = "docman.storage.localOnly";
    private static final String LOCAL_ONLY_ENV = "DOCMAN_STORAGE_LOCAL_ONLY";
    private static final String PREFER_LOCAL_PROPERTY = "docman.storage.preferLocal";
    private static final String PREFER_LOCAL_ENV = "DOCMAN_STORAGE_PREFER_LOCAL";

    private final SysOssMapper sysOssMapper;

    @Override
    public boolean ensureDirectory(String path) {
        if (isLocalOnlyEnabled()) {
            return ensureLocalDirectory(path);
        }
        try {
            OssClient client = OssFactory.instance();
            String key = normalize(path) + "/.keep";
            client.upload(new ByteArrayInputStream(new byte[0]), key, 0L, "application/octet-stream");
            return true;
        } catch (Exception e) {
            log.warn("创建OSS存储目录失败，回退到本地目录: {}", path, e);
            return ensureLocalDirectory(path);
        }
    }

    @Override
    public byte[] load(String path) {
        if (isLocalOnlyEnabled()) {
            return readLocal(path);
        }
        try (var inputStream = OssFactory.instance().getObjectContent(normalize(path))) {
            return inputStream.readAllBytes();
        } catch (Exception e) {
            log.warn("读取OSS文件失败，回退到本地文件: {}", path, e);
            return readLocal(path);
        }
    }

    @Override
    public StoredDocument store(String path, byte[] content, String fileName, String contentType) {
        if (isLocalOnlyEnabled()) {
            writeLocal(path, content);
            return new StoredDocument(path, fileName, null);
        }
        try {
            OssClient client = OssFactory.instance();
            String key = normalize(path);
            UploadResult result = client.upload(new ByteArrayInputStream(content), key, (long) content.length, contentType);
            Long ossId = saveStorageRecord(client, result, fileName, contentType, content.length);
            return new StoredDocument(path, result.getFilename() != null ? result.getFilename() : fileName, ossId);
        } catch (Exception e) {
            log.warn("存储OSS文件失败，回退到本地文件: {}", path, e);
            writeLocal(path, content);
            return new StoredDocument(path, fileName, null);
        }
    }

    private Long saveStorageRecord(OssClient client, UploadResult result, String originalFileName,
                                   String contentType, int contentLength) {
        SysOssExt ext = new SysOssExt();
        ext.setFileSize((long) contentLength);
        ext.setContentType(contentType);

        SysOss oss = new SysOss();
        oss.setUrl(result.getUrl());
        oss.setFileName(result.getFilename() != null ? result.getFilename() : originalFileName);
        oss.setOriginalName(originalFileName);
        oss.setFileSuffix(resolveSuffix(originalFileName));
        oss.setService(client.getConfigKey());
        oss.setExt1(JsonUtils.toJsonString(ext));
        sysOssMapper.insert(oss);
        return oss.getOssId();
    }

    private String resolveSuffix(String originalFileName) {
        if (originalFileName == null) {
            return "";
        }
        int index = originalFileName.lastIndexOf('.');
        return index >= 0 ? originalFileName.substring(index) : "";
    }

    private String normalize(String path) {
        return path.startsWith("/") ? path.substring(1) : path;
    }

    private boolean ensureLocalDirectory(String path) {
        try {
            Files.createDirectories(resolveLocalPath(path));
            return true;
        } catch (IOException e) {
            log.error("创建本地目录失败: {}", path, e);
            return false;
        }
    }

    private byte[] readLocal(String path) {
        Path localPath = resolveLocalPath(path);
        if (!Files.exists(localPath)) {
            throw new IllegalStateException("本地文件不存在: " + localPath);
        }
        try {
            return Files.readAllBytes(localPath);
        } catch (IOException e) {
            throw new IllegalStateException("读取本地文件失败: " + localPath, e);
        }
    }

    private void writeLocal(String path, byte[] content) {
        Path localPath = resolveLocalPath(path);
        try {
            Path parent = localPath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.write(localPath, content, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            throw new IllegalStateException("写入本地文件失败: " + localPath, e);
        }
    }

    private Path resolveLocalPath(String path) {
        Path localRoot = getLocalRoot();
        String relative = path.startsWith("/") ? path.substring(1) : path;
        if (relative.isBlank()) {
            throw new IllegalStateException("存储路径为空");
        }
        Path localPath = localRoot.resolve(relative.replace("/", File.separator)).normalize();
        if (!localPath.startsWith(localRoot)) {
            throw new IllegalStateException("非法存储路径: " + path);
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

    private boolean isLocalOnlyEnabled() {
        String flag = System.getProperty(LOCAL_ONLY_PROPERTY);
        if (flag == null || flag.isBlank()) {
            flag = System.getProperty(PREFER_LOCAL_PROPERTY);
        }
        if (flag == null || flag.isBlank()) {
            flag = System.getenv(LOCAL_ONLY_ENV);
        }
        if (flag == null || flag.isBlank()) {
            flag = System.getenv(PREFER_LOCAL_ENV);
        }
        return Boolean.parseBoolean(flag);
    }
}
