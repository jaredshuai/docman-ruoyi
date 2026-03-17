package org.dromara.docman.infrastructure.workflow;

import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.enums.BusinessStatusEnum;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.port.InfrastructureAdapter;
import org.dromara.docman.application.port.out.ProcessEnginePort;
import org.dromara.warm.flow.core.FlowEngine;
import org.dromara.warm.flow.core.dto.FlowParams;
import org.dromara.warm.flow.core.entity.Definition;
import org.dromara.warm.flow.core.entity.Instance;

@Slf4j
@InfrastructureAdapter("Warm-Flow 流程引擎")
public class WarmFlowProcessEngineAdapter implements ProcessEnginePort {

    @Override
    public Long startProcess(Long definitionId, String businessId, Long starterUserId) {
        Definition definition = FlowEngine.defService().getById(definitionId);
        if (definition == null) {
            throw new ServiceException("流程定义不存在: " + definitionId);
        }
        FlowParams flowParams = FlowParams.build()
            .handler(String.valueOf(starterUserId))
            .flowCode(definition.getFlowCode())
            .flowStatus(BusinessStatusEnum.DRAFT.getStatus());
        Instance instance = FlowEngine.insService().start(businessId, flowParams);
        log.info("Warm-Flow 启动流程成功: definitionId={}, businessId={}, starterUserId={}, instanceId={}", definitionId, businessId, starterUserId, instance.getId());
        return instance.getId();
    }
}
