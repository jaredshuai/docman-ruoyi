package org.dromara.docman.application.service;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.enums.BusinessStatusEnum;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.domain.event.WorkflowNodeFinishedEvent;
import org.dromara.docman.context.NodeContextReader;
import org.dromara.docman.domain.entity.DocNodeContext;
import org.dromara.docman.domain.entity.DocProcessConfig;
import org.dromara.docman.domain.enums.DocProcessConfigStatus;
import org.dromara.docman.domain.service.DocProcessStateMachine;
import org.dromara.docman.domain.vo.DocProjectVo;
import org.dromara.docman.plugin.DocumentPlugin;
import org.dromara.docman.plugin.PluginContext;
import org.dromara.docman.plugin.PluginRegistry;
import org.dromara.docman.plugin.PluginResult;
import org.dromara.docman.plugin.runtime.PluginExecutionRequest;
import org.dromara.docman.plugin.runtime.PluginExecutor;
import org.dromara.docman.plugin.runtime.PluginExecutionResult;
import org.dromara.docman.service.IDocDocumentRecordService;
import org.dromara.docman.service.IDocProcessConfigService;
import org.dromara.docman.service.IDocProjectService;
import org.dromara.docman.service.INodeContextService;
import org.dromara.warm.flow.core.FlowEngine;
import org.dromara.warm.flow.core.entity.Node;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocWorkflowNodeApplicationService {

    private final PluginRegistry pluginRegistry;
    private final PluginExecutor pluginExecutor;
    private final INodeContextService contextService;
    private final IDocProcessConfigService processConfigService;
    private final IDocProjectService projectService;
    private final IDocDocumentRecordService documentRecordService;

    /**
     * 处理工作流节点完成事件。
     * <p>现在使用框架层结构化解析后的 {@link WorkflowNodeFinishedEvent.NodeExtPayload}，
     * 不再自行解析 JSON。
     */
    public void handleNodeFinished(WorkflowNodeFinishedEvent event) {
        WorkflowNodeFinishedEvent.NodeExtPayload parsedExt = event.getParsedExt();
        List<WorkflowNodeFinishedEvent.PluginBinding> pluginBindings = parsedExt.getPlugins();
        if (!pluginBindings.isEmpty()) {
            DocProcessConfig config = processConfigService.queryByInstanceId(event.getInstanceId());
            if (config == null) {
                log.warn("未找到流程实例对应的项目配置: instanceId={}", event.getInstanceId());
            } else {
                DocProjectVo project = projectService.queryById(config.getProjectId());
                if (project == null) {
                    log.warn("未找到项目: projectId={}", config.getProjectId());
                } else {
                    executePluginBindings(project.getId(), project.getName(), event.getInstanceId(), event.getNodeCode(),
                        project.getNasBasePath(), parsedExt.getArchiveFolderName(), pluginBindings, false);
                }
            }
        }

        handleWorkflowCompleted(event);
    }

    /**
     * 在运行中的流程实例上手动触发指定节点的插件。
     *
     * @param config   流程配置
     * @param nodeCode 节点编码，为空时触发定义下全部节点
     */
    public void triggerPlugins(DocProcessConfig config, String nodeCode) {
        DocProjectVo project = projectService.queryById(config.getProjectId());
        if (project == null) {
            throw new ServiceException("项目不存在: " + config.getProjectId());
        }

        List<Node> nodes = resolveTriggerNodes(config.getDefinitionId(), nodeCode);
        if (nodes.isEmpty()) {
            throw new ServiceException("未找到可触发的流程节点");
        }

        for (Node node : nodes) {
            WorkflowNodeFinishedEvent.NodeExtPayload parsedExt = parseNodeExt(node.getExt());
            if (parsedExt.getPlugins().isEmpty()) {
                continue;
            }
            executePluginBindings(project.getId(), project.getName(), config.getInstanceId(), node.getNodeCode(),
                project.getNasBasePath(), parsedExt.getArchiveFolderName(), parsedExt.getPlugins(), false);
        }
    }

    /**
     * 为项目运行时上下文手动触发一组已绑定插件。
     *
     * @param projectId          项目ID
     * @param projectName        项目名称
     * @param processInstanceId  运行时实例ID
     * @param nodeCode           节点编码
     * @param nasBasePath        项目 NAS 根路径
     * @param archiveFolderName  归档目录名
     * @param pluginIds          插件编码列表
     * @return 每个插件的执行结果
     */
    public List<PluginExecutionResult> triggerBoundPlugins(Long projectId, String projectName, Long processInstanceId,
                                                           String nodeCode, String nasBasePath,
                                                           String archiveFolderName, List<String> pluginIds) {
        List<WorkflowNodeFinishedEvent.PluginBinding> bindings = new ArrayList<>(pluginIds.size());
        for (String pluginId : pluginIds) {
            WorkflowNodeFinishedEvent.PluginBinding binding = new WorkflowNodeFinishedEvent.PluginBinding();
            binding.setPluginId(pluginId);
            binding.setConfig(Map.of());
            bindings.add(binding);
        }
        return executePluginBindings(projectId, projectName, processInstanceId, nodeCode, nasBasePath,
            archiveFolderName, bindings, true);
    }

    private void handleWorkflowCompleted(WorkflowNodeFinishedEvent event) {
        if (!BusinessStatusEnum.FINISH.getStatus().equals(event.getStatus())) {
            return;
        }
        DocProcessConfig config = processConfigService.queryByInstanceId(event.getInstanceId());
        if (config == null) {
            return;
        }
        DocProcessConfigStatus currentStatus = DocProcessConfigStatus.of(config.getStatus());
        DocProcessStateMachine.checkTransition(currentStatus, DocProcessConfigStatus.COMPLETED);
        processConfigService.updateStatus(config.getId(), DocProcessConfigStatus.COMPLETED.getCode());
        log.info("文档流程配置已完成: configId={}, instanceId={}", config.getId(), event.getInstanceId());
    }

    private List<PluginExecutionResult> executePluginBindings(Long projectId, String projectName, Long processInstanceId,
                                                              String nodeCode, String nasBasePath,
                                                              String archiveFolderName,
                                                              List<WorkflowNodeFinishedEvent.PluginBinding> bindings,
                                                              boolean failOnMissingPlugin) {
        DocNodeContext nodeContext = contextService.getOrCreate(processInstanceId, nodeCode, projectId);
        NodeContextReader reader = contextService.buildReader(processInstanceId);
        List<PluginExecutionResult> results = new ArrayList<>(bindings.size());
        for (WorkflowNodeFinishedEvent.PluginBinding binding : bindings) {
            DocumentPlugin plugin = pluginRegistry.getPlugin(binding.getPluginId());
            if (plugin == null) {
                if (failOnMissingPlugin) {
                    throw new ServiceException("插件未注册: " + binding.getPluginId());
                }
                log.error("插件未注册: {}", binding.getPluginId());
                continue;
            }
            PluginContext ctx = PluginContext.builder()
                .projectId(projectId)
                .projectName(projectName)
                .processInstanceId(processInstanceId)
                .nodeCode(nodeCode)
                .contextReader(reader)
                .processWriter((field, value) -> contextService.putProcessVariable(nodeContext.getId(), field, value))
                .nodeWriter((field, value) -> contextService.putNodeVariable(nodeContext.getId(), field, value))
                .factWriter((field, value) -> contextService.putDocumentFact(nodeContext.getId(), field, value))
                .contentWriter((key, text) -> contextService.putUnstructuredContent(nodeContext.getId(), key, text))
                .pluginConfig(binding.getConfig() == null ? Map.of() : new LinkedHashMap<>(binding.getConfig()))
                .nasBasePath(nasBasePath)
                .archiveFolderName(archiveFolderName)
                .build();
            results.add(executePlugin(projectId, plugin, ctx));
        }
        return results;
    }

    private PluginExecutionResult executePlugin(Long projectId, DocumentPlugin plugin, PluginContext ctx) {
        PluginExecutionResult executionResult = pluginExecutor.execute(
            PluginExecutionRequest.builder().plugin(plugin).context(ctx).build()
        );
        PluginResult result = executionResult.getResult();
        if (!result.isSuccess()) {
            log.error("插件执行失败: {} - {}, cost={}ms", plugin.getPluginId(), result.getErrorMessage(), executionResult.getCostMs());
            return executionResult;
        }

        if (result.getGeneratedFiles() != null) {
            if (!result.getGeneratedFiles().isEmpty()) {
                documentRecordService.markLatestUniquePluginArtifactsObsolete(projectId, plugin.getPluginId());
            }
            for (PluginResult.GeneratedFile file : result.getGeneratedFiles()) {
                documentRecordService.recordPluginGenerated(projectId, plugin.getPluginId(), file);
            }
        }
        log.info("插件执行成功: {}, cost={}ms", plugin.getPluginId(), executionResult.getCostMs());
        return executionResult;
    }

    private List<Node> resolveTriggerNodes(Long definitionId, String nodeCode) {
        if (StrUtil.isNotBlank(nodeCode)) {
            Node node = FlowEngine.nodeService().getByDefIdAndNodeCode(definitionId, nodeCode);
            if (node == null) {
                throw new ServiceException("节点不存在: " + nodeCode);
            }
            return List.of(node);
        }
        return FlowEngine.nodeService().getByDefId(definitionId);
    }

    private WorkflowNodeFinishedEvent.NodeExtPayload parseNodeExt(String nodeExt) {
        if (StrUtil.isBlank(nodeExt)) {
            return WorkflowNodeFinishedEvent.NodeExtPayload.EMPTY;
        }
        try {
            JSONObject extJson = JSONUtil.parseObj(nodeExt);
            WorkflowNodeFinishedEvent.NodeExtPayload payload = new WorkflowNodeFinishedEvent.NodeExtPayload();
            payload.setArchiveFolderName(extJson.getStr("archiveFolderName", ""));
            payload.setPlugins(parsePlugins(extJson.getJSONArray("plugins")));
            @SuppressWarnings("unchecked")
            Map<String, Object> extra = (Map<String, Object>) extJson.get("extra");
            payload.setExtra(extra);
            return payload;
        } catch (Exception e) {
            log.warn("[DocWorkflowNodeApplicationService] 解析节点ext失败: {}", e.getMessage());
            return WorkflowNodeFinishedEvent.NodeExtPayload.EMPTY;
        }
    }

    private List<WorkflowNodeFinishedEvent.PluginBinding> parsePlugins(JSONArray pluginsArray) {
        if (pluginsArray == null || pluginsArray.isEmpty()) {
            return List.of();
        }
        List<WorkflowNodeFinishedEvent.PluginBinding> bindings = new ArrayList<>(pluginsArray.size());
        for (int index = 0; index < pluginsArray.size(); index++) {
            JSONObject pluginJson = pluginsArray.getJSONObject(index);
            WorkflowNodeFinishedEvent.PluginBinding binding = new WorkflowNodeFinishedEvent.PluginBinding();
            binding.setPluginId(pluginJson.getStr("pluginId"));
            @SuppressWarnings("unchecked")
            Map<String, Object> config = (Map<String, Object>) pluginJson.get("config");
            binding.setConfig(config);
            bindings.add(binding);
        }
        return bindings;
    }
}
