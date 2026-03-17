package org.dromara.docman.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.docman.application.port.out.DocumentStoragePort;
import org.dromara.docman.domain.service.DocPathResolver;
import org.dromara.docman.service.IDocNasService;
import org.springframework.stereotype.Service;

/**
 * @deprecated 已迁移为 DocumentStoragePort + DocPathResolver，此实现仅做兼容委托。
 */
@Slf4j
@Service
@Deprecated(forRemoval = true)
@RequiredArgsConstructor
public class DocNasServiceImpl implements IDocNasService {

    private final DocumentStoragePort documentStoragePort;
    private final DocPathResolver docPathResolver;

    @Override
    public String buildProjectBasePath(String customerType, String projectName) {
        return docPathResolver.buildProjectBasePath(customerType, projectName);
    }

    @Override
    public boolean createProjectDirectory(String basePath) {
        return documentStoragePort.ensureDirectory(basePath);
    }

    @Override
    public boolean createNodeDirectory(String basePath, String folderName) {
        return documentStoragePort.ensureDirectory(basePath + "/" + folderName);
    }

    @Override
    public Long uploadFile(String nasPath, byte[] fileBytes, String fileName) {
        DocumentStoragePort.StoredDocument doc = documentStoragePort.store(nasPath, fileBytes, fileName, "application/octet-stream");
        return doc.storageRecordId();
    }
}
