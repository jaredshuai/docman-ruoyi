package org.dromara.docman.infrastructure.storage;

import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.port.InfrastructureAdapter;
import org.dromara.common.oss.core.OssClient;
import org.dromara.common.oss.entity.UploadResult;
import org.dromara.common.oss.factory.OssFactory;
import org.dromara.docman.application.port.out.DocumentStoragePort;

import java.io.ByteArrayInputStream;

@Slf4j
@InfrastructureAdapter("S3/OSS 文档存储")
public class OssDocumentStorageAdapter implements DocumentStoragePort {

    @Override
    public boolean ensureDirectory(String path) {
        try {
            OssClient client = OssFactory.instance();
            String key = normalize(path) + "/.keep";
            client.upload(new ByteArrayInputStream(new byte[0]), key, 0L, "application/octet-stream");
            return true;
        } catch (Exception e) {
            log.error("创建存储目录失败: {}", path, e);
            return false;
        }
    }

    @Override
    public byte[] load(String path) {
        try (var inputStream = OssFactory.instance().getObjectContent(normalize(path))) {
            return inputStream.readAllBytes();
        } catch (Exception e) {
            log.error("读取存储文件失败: {}", path, e);
            throw new IllegalStateException("读取存储文件失败: " + path, e);
        }
    }

    @Override
    public StoredDocument store(String path, byte[] content, String fileName, String contentType) {
        try {
            OssClient client = OssFactory.instance();
            String key = normalize(path);
            UploadResult result = client.upload(new ByteArrayInputStream(content), key, (long) content.length, contentType);
            return new StoredDocument(path, result.getFilename() != null ? result.getFilename() : fileName, null);
        } catch (Exception e) {
            log.error("存储文件失败: {}", path, e);
            throw e;
        }
    }

    private String normalize(String path) {
        return path.startsWith("/") ? path.substring(1) : path;
    }
}
