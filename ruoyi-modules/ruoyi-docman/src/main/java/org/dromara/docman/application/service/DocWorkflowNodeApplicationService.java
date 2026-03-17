package org.dromara.docman.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.domain.event.WorkflowNodeFinishedEvent;
import org.dromara.docman.context.NodeContextReader;
import org.dromara.docman.domain.entity.DocDocumentRecord;
import org.dromara.docman.domain.entity.DocNodeContext;
import org.dromara.docman.domain.entity.DocProcessConfig;
import org.dromara.docman.domain.entity.DocProject;
import org.dromara.docman.domain.enums.DocDocumentStatus;
import org.dromara.docman.plugin.DocumentPlugin;
import org.dromara.docman.plugin.PluginContext;
import org.dromara.docman.plugin.PluginRegistry;
import org.dromara.docman.plugin.PluginResult;
import org.dromara.docman.plugin.runtime.PluginExecutionRequest;
import org.dromara.docman.plugin.runtime.PluginExecutor;
import org.dromara.docman.plugin.runtime.PluginExecutionResult;
import org.dromara.docman.service.IDocDocumentRecordService;
import org.dromara.docman.service.IDocProcessService;
import org.dromara.docman.service.IDocProjectService;
import org.dromara.docman.service.INodeContextService;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocWorkflowNodeApplicationService {

    private final PluginRegistry pluginRegistry;
    private final PluginExecutor pluginExecutor;
    private final INodeContextService contextService;
    private final IDocProcessService processService;
    private final IDocProjectService projectService;
    private final IDocDocumentRecordService documentRecordService;

    /**
     * 处理工作流节点完成事件。
     * <p>使用框架层结构化解析后的 {@link WorkflowNodeFinishedEvent.NodeExtPayload}，
     * 不再自行解析 JSON。通过 Service 层访问数据，不直接注入 Mapper。
     */
    public void handleNodeFinished(WorkflowNodeFinishedEvent event) {
        WorkflowNodeFinishedEvent.NodeExtPayload parsedExt = event.getParsedExt();
        List<WorkflowNodeFinishedEvent.PluginBinding> pluginBindings = parsedExt.getPlugins();
        if (pluginBindings.isEmpty()) {
            return;
        }

        DocProcessConfig config = processService.getByInstanceId(event.getInstanceId());
        if (config == null) {
            log.warn("未找到流程实例对应的项目配置: instanceId={}", event.getInstanceId());
            return;
        }

        DocProject project = projectService.getProjectEntity(config.getProjectId());
        if (project == null) {
            log.warn("未找到项目: projectId={}", config.getProjectId());
            return;
        }

        DocNodeContext nodeContext = contextService.getOrCreate(event.getInstanceId(), event.getNodeCode(), project.getId());
        NodeContextReader reader = contextService.buildReader(event.getInstanceId());
        String archiveFolderName = parsedExt.getArchiveFolderName();

        for (WorkflowNodeFinishedEvent.PluginBinding binding : pluginBindings) {
            String pluginId = binding.getPluginId();
            Map<String, Object> pluginConfig = binding.getConfig();

            DocumentPlugin plugin = pluginRegistry.getPlugin(pluginId);
            if (plugin == null) {
                log.error("插件未注册: {}", pluginId);
                continue;
            }

            PluginContext ctx = PluginContext.builder()
                .projectId(project.getId())
                .projectName(project.getName())
                .processInstanceId(event.getInstanceId())
                .nodeCode(event.getNodeCode())
                .contextReader(reader)
                .processWriter((field, value) -> contextService.putProcessVariable(nodeContext.getId(), field, value))
                .nodeWriter((field, value) -> contextService.putNodeVariable(nodeContext.getId(), field, value))
                .factWriter((field, value) -> contextService.putDocumentFact(nodeContext.getId(), field, value))
                .contentWriter((key, text) -> contextService.putUnstructuredContent(nodeContext.getId(), key, text))
                .pluginConfig(pluginConfig)
                .nasBasePath(project.getNasBasePath())
                .archiveFolderName(archiveFolderName)
                .build();

            PluginExecutionResult executionResult = pluginExecutor.execute(
                PluginExecutionRequest.builder().plugin(plugin).context(ctx).build()
            );
            PluginResult result = executionResult.getResult();
            if (!result.isSuccess()) {
                log.error("插件执行失败: {} - {}, cost={}ms", pluginId, result.getErrorMessage(), executionResult.getCostMs());
                continue;
            }

            if (result.getGeneratedFiles() != null) {
                for (PluginResult.GeneratedFile file : result.getGeneratedFiles()) {
                    DocDocumentRecord record = new DocDocumentRecord();
                    record.setProjectId(project.getId());
                    record.setPluginId(pluginId);
                    record.setSourceType("plugin");
                    record.setFileName(file.getFileName());
                    record.setNasPath(file.getNasPath());
                    record.setOssId(file.getOssId());
                    record.setStatus(DocDocumentStatus.GENERATED.getCode());
                    record.setGeneratedAt(new Date());
                    documentRecordService.recordPluginGenerated(record);
                }
            }
            log.info("插件执行成功: {}, cost={}ms", pluginId, executionResult.getCostMs());
        }
    }
}
