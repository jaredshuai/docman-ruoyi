package org.dromara.docman.application.service;

import lombok.RequiredArgsConstructor;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.docman.application.assembler.DocPluginAssembler;
import org.dromara.docman.domain.vo.DocPluginExecutionLogVo;
import org.dromara.docman.domain.vo.DocPluginInfoVo;
import org.dromara.docman.plugin.PluginRegistry;
import org.dromara.docman.service.IDocPluginExecutionLogService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DocPluginApplicationService {

    private final PluginRegistry pluginRegistry;
    private final IDocPluginExecutionLogService pluginExecutionLogService;
    private final DocPluginAssembler pluginAssembler;

    public List<DocPluginInfoVo> listPlugins() {
        return pluginRegistry.getAllPlugins().values().stream()
            .map(pluginAssembler::toInfoVo)
            .toList();
    }

    public TableDataInfo<DocPluginExecutionLogVo> listExecutionLogs(Long projectId, Long processInstanceId,
                                                                    String nodeCode, String pluginId,
                                                                    PageQuery pageQuery) {
        return pluginExecutionLogService.queryPageList(projectId, processInstanceId, nodeCode, pluginId, pageQuery);
    }
}
