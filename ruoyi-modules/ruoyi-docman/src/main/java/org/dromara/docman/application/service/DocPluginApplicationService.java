package org.dromara.docman.application.service;

import lombok.RequiredArgsConstructor;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.docman.application.assembler.DocPluginAssembler;
import org.dromara.docman.domain.bo.DocPluginTriggerBo;
import org.dromara.docman.domain.entity.DocProcessConfig;
import org.dromara.docman.domain.enums.DocProcessConfigStatus;
import org.dromara.docman.domain.vo.DocPluginExecutionLogVo;
import org.dromara.docman.domain.vo.DocPluginInfoVo;
import org.dromara.docman.plugin.PluginRegistry;
import org.dromara.docman.service.IDocProcessConfigService;
import org.dromara.docman.service.IDocPluginExecutionLogService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DocPluginApplicationService {

    private final PluginRegistry pluginRegistry;
    private final IDocPluginExecutionLogService pluginExecutionLogService;
    private final IDocProcessConfigService processConfigService;
    private final DocWorkflowNodeApplicationService workflowNodeApplicationService;
    private final DocPluginAssembler pluginAssembler;

    public List<DocPluginInfoVo> listPlugins() {
        return pluginRegistry.getAllPlugins().values().stream()
            .map(pluginAssembler::toInfoVo)
            .toList();
    }

    /**
     * 分页查询插件执行日志（列表不含快照字段）
     */
    public TableDataInfo<DocPluginExecutionLogVo> listExecutionLogs(Long projectId, Long processInstanceId,
                                                                    String nodeCode, String pluginId,
                                                                    PageQuery pageQuery) {
        return pluginExecutionLogService.queryPageList(projectId, processInstanceId, nodeCode, pluginId, pageQuery);
    }

    /**
     * 查询插件执行日志详情（包含完整快照字段）
     */
    public DocPluginExecutionLogVo getExecutionLogById(Long id) {
        return pluginExecutionLogService.queryById(id);
    }

    public void triggerPlugin(DocPluginTriggerBo bo) {
        DocProcessConfig processConfig = processConfigService.queryByInstanceId(bo.getProcessInstanceId());
        if (processConfig == null) {
            throw new ServiceException("流程实例不存在或未关联文档流程配置");
        }
        if (!DocProcessConfigStatus.RUNNING.getCode().equals(processConfig.getStatus())) {
            throw new ServiceException("仅运行中的流程实例允许手动触发插件");
        }
        workflowNodeApplicationService.triggerPlugins(processConfig, bo.getNodeCode());
    }
}
