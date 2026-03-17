package org.dromara.docman.application.port.out;

import org.dromara.common.core.port.OutboundPort;

import java.io.InputStream;

@OutboundPort("S3/OSS 文档存储")
public interface DocumentStoragePort {

    boolean ensureDirectory(String path);

    StoredDocument store(String path, byte[] content, String fileName, String contentType);

    /**
     * 从存储中下载文件，返回输入流；调用方负责关闭流
     */
    InputStream download(String path);

    record StoredDocument(String path, String fileName, Long storageRecordId) {
    }
}
