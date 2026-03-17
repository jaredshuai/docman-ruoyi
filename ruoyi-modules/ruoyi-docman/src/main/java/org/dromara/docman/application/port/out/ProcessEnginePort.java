package org.dromara.docman.application.port.out;

import org.dromara.common.core.port.OutboundPort;

@OutboundPort("Warm-Flow 流程引擎")
public interface ProcessEnginePort {

    Long startProcess(Long definitionId, String businessId, Long starterUserId);
}
