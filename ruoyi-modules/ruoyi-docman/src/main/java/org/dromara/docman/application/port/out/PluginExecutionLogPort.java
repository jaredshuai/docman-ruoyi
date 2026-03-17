package org.dromara.docman.application.port.out;

import org.dromara.common.core.port.OutboundPort;
import org.dromara.docman.domain.entity.DocPluginExecutionLog;

@OutboundPort("插件执行日志持久化")
public interface PluginExecutionLogPort {

    void save(DocPluginExecutionLog executionLog);
}
