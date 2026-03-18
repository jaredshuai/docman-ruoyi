package org.dromara.docman.service;

import org.dromara.docman.domain.entity.DocArchivePackage;

import java.util.List;

public interface IDocArchiveService {

    DocArchivePackage archiveProject(Long projectId);

    DocArchivePackage getById(Long archiveId);

    DocArchivePackage getByProjectId(Long projectId);

    List<DocArchivePackage> listByProjectId(Long projectId);

    long countByProjectId(Long projectId);
}
