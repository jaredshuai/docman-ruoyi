package org.dromara.docman.service;

import org.dromara.docman.domain.entity.DocProcessConfig;

public interface IDocProcessService {

    void bindProcess(Long projectId, Long definitionId);

    Long startProcess(Long projectId);

    DocProcessConfig getByProjectId(Long projectId);

    /**
     * 根据流程实例ID查询关联的流程配置（系统内部调用，不做权限校验）
     */
    DocProcessConfig getByInstanceId(Long instanceId);
}
