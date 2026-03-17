package org.dromara.docman.service;

/**
 * @deprecated 已迁移为 application.port.out.DocumentStoragePort + DocPathResolver。
 */
@Deprecated(forRemoval = true)
public interface IDocNasService {

    String buildProjectBasePath(String customerType, String projectName);

    boolean createProjectDirectory(String basePath);

    boolean createNodeDirectory(String basePath, String folderName);

    Long uploadFile(String nasPath, byte[] fileBytes, String fileName);
}
