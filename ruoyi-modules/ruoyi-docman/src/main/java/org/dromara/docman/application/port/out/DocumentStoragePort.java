package org.dromara.docman.application.port.out;

import org.dromara.common.core.port.OutboundPort;

@OutboundPort("S3/OSS 文档存储")
public interface DocumentStoragePort {

    boolean ensureDirectory(String path);

    byte[] load(String path);

    StoredDocument store(String path, byte[] content, String fileName, String contentType);

    record StoredDocument(String path, String fileName, Long storageRecordId) {
    }
}
