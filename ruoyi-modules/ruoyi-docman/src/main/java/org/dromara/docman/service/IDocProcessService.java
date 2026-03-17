package org.dromara.docman.service;

import org.dromara.docman.domain.entity.DocProcessConfig;

public interface IDocProcessService {

    void bindProcess(Long projectId, Long definitionId);

    Long startProcess(Long projectId);

    DocProcessConfig getByProjectId(Long projectId);
}
