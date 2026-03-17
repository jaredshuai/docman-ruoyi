package org.dromara.docman.infrastructure.log;

import lombok.RequiredArgsConstructor;
import org.dromara.common.core.port.InfrastructureAdapter;
import org.dromara.docman.application.port.out.PluginExecutionLogPort;
import org.dromara.docman.domain.entity.DocPluginExecutionLog;
import org.dromara.docman.mapper.DocPluginExecutionLogMapper;

@InfrastructureAdapter("插件执行日志持久化")
@RequiredArgsConstructor
public class MybatisPluginExecutionLogAdapter implements PluginExecutionLogPort {

    private final DocPluginExecutionLogMapper pluginExecutionLogMapper;

    @Override
    public void save(DocPluginExecutionLog executionLog) {
        pluginExecutionLogMapper.insert(executionLog);
    }
}
