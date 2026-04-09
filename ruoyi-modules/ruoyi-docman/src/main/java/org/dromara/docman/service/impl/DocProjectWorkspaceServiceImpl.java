package org.dromara.docman.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.docman.application.service.DocWorkflowNodeApplicationService;
import org.dromara.docman.domain.bo.DocProjectAdvanceNodeBo;
import org.dromara.docman.domain.bo.DocProjectNodeTaskCompleteBo;
import org.dromara.docman.domain.entity.DocProject;
import org.dromara.docman.domain.entity.DocProjectDrawing;
import org.dromara.docman.domain.entity.DocProjectEstimateSnapshot;
import org.dromara.docman.domain.entity.DocProjectType;
import org.dromara.docman.domain.entity.DocProjectNodeTaskRuntime;
import org.dromara.docman.domain.entity.DocProjectRuntime;
import org.dromara.docman.domain.entity.DocProjectVisa;
import org.dromara.docman.domain.entity.DocWorkflowNodeTask;
import org.dromara.docman.domain.entity.DocWorkflowTemplate;
import org.dromara.docman.domain.entity.DocWorkflowTemplateNode;
import org.dromara.docman.domain.enums.DocProjectAction;
import org.dromara.docman.domain.enums.DocProjectNodeTaskStatus;
import org.dromara.docman.domain.enums.DocProjectRuntimeStatus;
import org.dromara.docman.domain.enums.DocWorkflowTaskCompletionRule;
import org.dromara.docman.domain.service.DocProjectRuntimeStateMachine;
import org.dromara.docman.domain.vo.DocProjectNodeTaskRuntimeVo;
import org.dromara.docman.domain.vo.DocProjectEstimateSnapshotVo;
import org.dromara.docman.domain.vo.DocProjectWorkspaceVo;
import org.dromara.docman.domain.vo.DocWorkflowNodeTaskVo;
import org.dromara.docman.domain.vo.DocWorkflowTemplateNodeVo;
import org.dromara.docman.mapper.DocProjectDrawingMapper;
import org.dromara.docman.mapper.DocProjectEstimateSnapshotMapper;
import org.dromara.docman.mapper.DocProjectMapper;
import org.dromara.docman.mapper.DocProjectNodeTaskRuntimeMapper;
import org.dromara.docman.mapper.DocProjectRuntimeMapper;
import org.dromara.docman.mapper.DocProjectTypeMapper;
import org.dromara.docman.mapper.DocProjectVisaMapper;
import org.dromara.docman.mapper.DocWorkflowNodeTaskMapper;
import org.dromara.docman.mapper.DocWorkflowTemplateMapper;
import org.dromara.docman.mapper.DocWorkflowTemplateNodeMapper;
import org.dromara.docman.service.IDocProjectAccessService;
import org.dromara.docman.service.IDocProjectWorkspaceService;
import org.dromara.docman.service.INodeContextService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DocProjectWorkspaceServiceImpl implements IDocProjectWorkspaceService {

    private static final String AUTO_EVIDENCE_PREFIX = "auto:";
    private static final String INITIAL_ESTIMATE_TYPE = "initial_estimate";
    private static final String EXPORT_TEXT_NODE_CODE = "export_text";
    private static final String EXPORT_PLUGIN_ID = "telecom-export-text-mock";

    private final IDocProjectAccessService projectAccessService;
    private final DocProjectMapper projectMapper;
    private final DocProjectRuntimeMapper runtimeMapper;
    private final DocProjectTypeMapper projectTypeMapper;
    private final DocWorkflowTemplateMapper templateMapper;
    private final DocWorkflowTemplateNodeMapper nodeMapper;
    private final DocWorkflowNodeTaskMapper taskMapper;
    private final DocProjectNodeTaskRuntimeMapper taskRuntimeMapper;
    private final DocProjectDrawingMapper drawingMapper;
    private final DocProjectVisaMapper visaMapper;
    private final DocProjectEstimateSnapshotMapper estimateSnapshotMapper;
    private final INodeContextService nodeContextService;
    private final DocWorkflowNodeApplicationService workflowNodeApplicationService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DocProjectWorkspaceVo getWorkspace(Long projectId) {
        projectAccessService.assertAction(projectId, DocProjectAction.VIEW_PROJECT);
        DocProject project = requiredProject(projectId);
        DocProjectRuntime runtime = getOrInitRuntime(project);
        syncNodeTaskStatuses(project, runtime, runtime.getCurrentNodeCode());
        DocWorkflowTemplate template = templateMapper.selectById(runtime.getWorkflowTemplateId());
        if (template == null) {
            throw new ServiceException("项目未绑定有效的流程模板");
        }
        List<DocWorkflowTemplateNode> nodes = nodeMapper.selectList(new LambdaQueryWrapper<DocWorkflowTemplateNode>()
            .eq(DocWorkflowTemplateNode::getTemplateId, template.getId())
            .orderByAsc(DocWorkflowTemplateNode::getSortOrder)
            .orderByAsc(DocWorkflowTemplateNode::getCreateTime));

        DocProjectWorkspaceVo vo = new DocProjectWorkspaceVo();
        vo.setProjectId(project.getId());
        vo.setProjectName(project.getName());
        vo.setProjectTypeCode(project.getProjectTypeCode());
        vo.setCurrentNodeCode(runtime.getCurrentNodeCode());
        vo.setCurrentNodeName(resolveNodeName(nodes, runtime.getCurrentNodeCode()));
        vo.setRuntimeStatus(runtime.getStatus());
        vo.setNodes(buildNodeVos(nodes));
        vo.setCurrentNodeTasks(buildTaskRuntimeVos(projectId, runtime.getCurrentNodeCode()));
        vo.setDrawingCount(drawingMapper.selectCount(new LambdaQueryWrapper<DocProjectDrawing>()
            .eq(DocProjectDrawing::getProjectId, projectId)));
        vo.setVisaCount(visaMapper.selectCount(new LambdaQueryWrapper<DocProjectVisa>()
            .eq(DocProjectVisa::getProjectId, projectId)));
        vo.setLatestEstimateSnapshot(queryLatestEstimateSnapshot(projectId));
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void completeTask(Long projectId, Long taskRuntimeId, DocProjectNodeTaskCompleteBo bo) {
        projectAccessService.assertAction(projectId, DocProjectAction.EDIT_PROJECT);
        DocProject project = requiredProject(projectId);
        DocProjectRuntime projectRuntime = getOrInitRuntime(project);
        DocProjectNodeTaskRuntime runtime = taskRuntimeMapper.selectById(taskRuntimeId);
        if (runtime == null || !projectId.equals(runtime.getProjectId())) {
            throw new ServiceException("节点事项不存在");
        }
        assertCurrentNodeTask(projectRuntime, runtime);
        DocWorkflowNodeTask definition = requiredTaskDefinition(projectRuntime.getWorkflowTemplateId(),
            runtime.getNodeCode(), runtime.getTaskCode());
        if (isPluginTask(definition)) {
            throw new ServiceException("插件事项请使用插件触发接口完成");
        }
        TaskCompletionEvaluation evaluation = evaluateTaskCompletion(project, definition);
        if (evaluation.autoManaged()) {
            if (!evaluation.completed()) {
                throw new ServiceException("当前事项需先满足业务数据条件后才能完成");
            }
            updateTaskRuntimeStatus(runtime, evaluation);
            return;
        }
        runtime.setStatus(DocProjectNodeTaskStatus.COMPLETED.getCode());
        runtime.setCompletedBy(LoginHelper.getUserId());
        runtime.setCompletedAt(new Date());
        runtime.setEvidenceRef(bo.getEvidenceRef());
        taskRuntimeMapper.updateById(runtime);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void triggerTaskPlugins(Long projectId, Long taskRuntimeId) {
        projectAccessService.assertAction(projectId, DocProjectAction.EDIT_PROJECT);
        DocProject project = requiredProject(projectId);
        DocProjectRuntime runtime = getOrInitRuntime(project);
        DocProjectNodeTaskRuntime taskRuntime = taskRuntimeMapper.selectById(taskRuntimeId);
        if (taskRuntime == null || !projectId.equals(taskRuntime.getProjectId())) {
            throw new ServiceException("节点事项不存在");
        }
        assertCurrentNodeTask(runtime, taskRuntime);
        DocWorkflowNodeTask definition = requiredTaskDefinition(runtime.getWorkflowTemplateId(),
            taskRuntime.getNodeCode(), taskRuntime.getTaskCode());
        if (!isPluginTask(definition)) {
            throw new ServiceException("当前事项未配置插件执行");
        }
        if (definition.getPluginCodes() == null || definition.getPluginCodes().isBlank()) {
            throw new ServiceException("该事项未绑定插件");
        }

        var contextEntity = nodeContextService.getOrCreate(runtime.getId(), taskRuntime.getNodeCode(), projectId);
        nodeContextService.putProcessVariable(contextEntity.getId(), "projectId", projectId);
        nodeContextService.putProcessVariable(contextEntity.getId(), "projectName", project.getName());
        nodeContextService.putProcessVariable(contextEntity.getId(), "projectTypeCode", project.getProjectTypeCode());
        nodeContextService.putProcessVariable(contextEntity.getId(), "drawingCount",
            drawingMapper.selectCount(new LambdaQueryWrapper<DocProjectDrawing>().eq(DocProjectDrawing::getProjectId, projectId)));
        nodeContextService.putProcessVariable(contextEntity.getId(), "visaCount",
            visaMapper.selectCount(new LambdaQueryWrapper<DocProjectVisa>().eq(DocProjectVisa::getProjectId, projectId)));
        List<String> pluginCodes = parsePluginCodes(definition.getPluginCodes());
        var results = workflowNodeApplicationService.triggerBoundPlugins(projectId, project.getName(), runtime.getId(),
            taskRuntime.getNodeCode(), resolveNasBasePath(project), taskRuntime.getNodeCode(), pluginCodes);
        for (var result : results) {
            if (!result.getResult().isSuccess()) {
                throw new ServiceException("插件执行失败: " + result.getResult().getErrorMessage());
            }
        }
        if (usesEstimateSnapshotRule(definition)) {
            saveLatestEstimateSnapshot(projectId, runtime.getId());
            syncNodeTaskStatuses(project, runtime, taskRuntime.getNodeCode());
            DocProjectNodeTaskRuntime refreshed = taskRuntimeMapper.selectById(taskRuntimeId);
            if (!DocProjectNodeTaskStatus.COMPLETED.getCode().equals(refreshed.getStatus())) {
                throw new ServiceException("估算结果未生成有效快照，事项未完成");
            }
            return;
        }
        markTaskCompleted(taskRuntime, "plugin:" + definition.getPluginCodes());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void triggerEstimate(Long projectId) {
        DocProject project = requiredProject(projectId);
        DocProjectRuntime runtime = getOrInitRuntime(project);
        syncNodeTaskStatuses(project, runtime, runtime.getCurrentNodeCode());
        List<DocProjectNodeTaskRuntime> runtimes = taskRuntimeMapper.selectList(new LambdaQueryWrapper<DocProjectNodeTaskRuntime>()
            .eq(DocProjectNodeTaskRuntime::getProjectId, projectId)
            .eq(DocProjectNodeTaskRuntime::getNodeCode, runtime.getCurrentNodeCode())
            .orderByAsc(DocProjectNodeTaskRuntime::getCreateTime));
        DocProjectNodeTaskRuntime estimateTaskRuntime = runtimes.stream()
            .filter(item -> usesEstimateSnapshotRule(requiredTaskDefinition(runtime.getWorkflowTemplateId(),
                item.getNodeCode(), item.getTaskCode())))
            .findFirst()
            .orElseThrow(() -> new ServiceException("当前节点不支持手动初步估算"));
        triggerTaskPlugins(projectId, estimateTaskRuntime.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void triggerExportText(Long projectId) {
        DocProject project = requiredProject(projectId);
        DocProjectRuntime runtime = getOrInitRuntime(project);
        syncNodeTaskStatuses(project, runtime, runtime.getCurrentNodeCode());
        if (!StringUtils.equals(EXPORT_TEXT_NODE_CODE, runtime.getCurrentNodeCode())) {
            throw new ServiceException("当前节点不允许触发文本导出");
        }
        if (queryLatestEstimateSnapshot(projectId) == null) {
            throw new ServiceException("请先完成初步估算后再导出文本");
        }
        DocProjectNodeTaskRuntime exportTaskRuntime = taskRuntimeMapper.selectList(new LambdaQueryWrapper<DocProjectNodeTaskRuntime>()
                .eq(DocProjectNodeTaskRuntime::getProjectId, projectId)
                .eq(DocProjectNodeTaskRuntime::getNodeCode, runtime.getCurrentNodeCode())
                .orderByAsc(DocProjectNodeTaskRuntime::getCreateTime))
            .stream()
            .filter(item -> {
                DocWorkflowNodeTask definition = requiredTaskDefinition(runtime.getWorkflowTemplateId(),
                    item.getNodeCode(), item.getTaskCode());
                return isPluginTask(definition) && parsePluginCodes(definition.getPluginCodes()).contains(EXPORT_PLUGIN_ID);
            })
            .findFirst()
            .orElseThrow(() -> new ServiceException("当前节点未配置文本导出事项"));
        triggerTaskPlugins(projectId, exportTaskRuntime.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void advanceNode(DocProjectAdvanceNodeBo bo) {
        projectAccessService.assertAction(bo.getProjectId(), DocProjectAction.EDIT_PROJECT);
        DocProject project = requiredProject(bo.getProjectId());
        DocProjectRuntime runtime = getOrInitRuntime(project);
        if (!runtime.getCurrentNodeCode().equals(bo.getCurrentNodeCode())) {
            throw new ServiceException("当前节点状态已变更，请刷新后重试");
        }
        syncNodeTaskStatuses(project, runtime, bo.getCurrentNodeCode());
        List<DocProjectNodeTaskRuntime> currentTasks = taskRuntimeMapper.selectList(new LambdaQueryWrapper<DocProjectNodeTaskRuntime>()
            .eq(DocProjectNodeTaskRuntime::getProjectId, bo.getProjectId())
            .eq(DocProjectNodeTaskRuntime::getNodeCode, bo.getCurrentNodeCode()));
        Map<String, DocWorkflowNodeTask> taskDefinitions = listTaskDefinitions(runtime.getWorkflowTemplateId(), bo.getCurrentNodeCode())
            .stream()
            .collect(HashMap::new, (map, item) -> map.put(item.getTaskCode(), item), HashMap::putAll);
        boolean allDone = currentTasks.stream()
            .filter(task -> isRequiredTask(taskDefinitions.get(task.getTaskCode())))
            .allMatch(task -> isFinishedTaskStatus(task.getStatus()));
        if (!allDone) {
            throw new ServiceException("当前节点仍有未完成事项，无法推进");
        }
        List<DocWorkflowTemplateNode> nodes = nodeMapper.selectList(new LambdaQueryWrapper<DocWorkflowTemplateNode>()
            .eq(DocWorkflowTemplateNode::getTemplateId, runtime.getWorkflowTemplateId())
            .orderByAsc(DocWorkflowTemplateNode::getSortOrder)
            .orderByAsc(DocWorkflowTemplateNode::getCreateTime));
        String nextNodeCode = resolveNextNodeCode(nodes, runtime.getCurrentNodeCode());
        if (nextNodeCode == null) {
            DocProjectRuntimeStateMachine.checkTransition(DocProjectRuntimeStatus.of(runtime.getStatus()), DocProjectRuntimeStatus.COMPLETED);
            runtime.setStatus(DocProjectRuntimeStatus.COMPLETED.getCode());
        } else {
            runtime.setCurrentNodeCode(nextNodeCode);
            initTaskRuntime(bo.getProjectId(), nextNodeCode);
        }
        runtimeMapper.updateById(runtime);
    }

    private DocProject requiredProject(Long projectId) {
        DocProject project = projectMapper.selectById(projectId);
        if (project == null) {
            throw new ServiceException("项目不存在");
        }
        return project;
    }

    private DocProjectRuntime getOrInitRuntime(DocProject project) {
        DocProjectRuntime runtime = runtimeMapper.selectOne(new LambdaQueryWrapper<DocProjectRuntime>()
            .eq(DocProjectRuntime::getProjectId, project.getId()));
        if (runtime != null) {
            return runtime;
        }
        ensureDefaultTelecomTemplate(project);
        DocWorkflowTemplate template = templateMapper.selectOne(new LambdaQueryWrapper<DocWorkflowTemplate>()
            .eq(DocWorkflowTemplate::getProjectTypeCode, project.getProjectTypeCode())
            .eq(DocWorkflowTemplate::getDefaultFlag, true)
            .last("limit 1"));
        if (template == null) {
            throw new ServiceException("项目类型未配置默认流程模板");
        }
        List<DocWorkflowTemplateNode> nodes = nodeMapper.selectList(new LambdaQueryWrapper<DocWorkflowTemplateNode>()
            .eq(DocWorkflowTemplateNode::getTemplateId, template.getId())
            .orderByAsc(DocWorkflowTemplateNode::getSortOrder)
            .orderByAsc(DocWorkflowTemplateNode::getCreateTime));
        if (nodes.isEmpty()) {
            throw new ServiceException("流程模板未配置节点");
        }
        DocProjectRuntime created = new DocProjectRuntime();
        created.setProjectId(project.getId());
        created.setWorkflowTemplateId(template.getId());
        created.setCurrentNodeCode(nodes.get(0).getNodeCode());
        created.setStatus(DocProjectRuntimeStatus.RUNNING.getCode());
        runtimeMapper.insert(created);
        initTaskRuntime(project.getId(), nodes.get(0).getNodeCode());
        return created;
    }

    private void ensureDefaultTelecomTemplate(DocProject project) {
        if (!"telecom".equals(project.getProjectTypeCode())) {
            return;
        }
        DocProjectType existingType = projectTypeMapper.selectOne(new LambdaQueryWrapper<DocProjectType>()
            .eq(DocProjectType::getCode, "telecom"));
        if (existingType == null) {
            DocProjectType type = new DocProjectType();
            type.setCode("telecom");
            type.setName("电信项目");
            type.setCustomerType("telecom");
            type.setDescription("电信项目默认类型");
            type.setSortOrder(1);
            type.setStatus("active");
            projectTypeMapper.insert(type);
        }
        DocWorkflowTemplate existingTemplate = templateMapper.selectOne(new LambdaQueryWrapper<DocWorkflowTemplate>()
            .eq(DocWorkflowTemplate::getProjectTypeCode, "telecom")
            .eq(DocWorkflowTemplate::getDefaultFlag, true)
            .last("limit 1"));
        if (existingTemplate != null) {
            return;
        }
        DocWorkflowTemplate template = new DocWorkflowTemplate();
        template.setCode("telecom-default");
        template.setName("电信项目默认流程");
        template.setProjectTypeCode("telecom");
        template.setDescription("电信项目 phase1 默认流程");
        template.setDefaultFlag(true);
        template.setSortOrder(1);
        template.setStatus("active");
        templateMapper.insert(template);

        createDefaultNode(template.getId(), 1, "project_info", "项目信息",
            List.of(task("project_info_fill", "完成项目信息", "form_fill", true, 1,
                    DocWorkflowTaskCompletionRule.PROJECT_BASIC_INFO_PRESENT.getCode(), null),
                task("project_info_confirm", "确认项目信息", "review_confirm", true, 2, null, null)));
        createDefaultNode(template.getId(), 2, "drawing_input", "图纸录入",
            List.of(task("drawing_fill", "录入图纸信息", "form_fill", true, 1,
                DocWorkflowTaskCompletionRule.DRAWING_EXISTS.getCode(), null)));
        createDefaultNode(template.getId(), 3, "visa_input", "签证录入",
            List.of(task("visa_fill", "录入签证信息", "form_fill", true, 1,
                DocWorkflowTaskCompletionRule.VISA_EXISTS.getCode(), null)));
        createDefaultNode(template.getId(), 4, "initial_estimate", "初步估算",
            List.of(task("estimate_run", "执行初步估算", "plugin_run", true, 1,
                DocWorkflowTaskCompletionRule.ESTIMATE_SNAPSHOT_EXISTS.getCode(), "telecom-estimate-mock")));
        createDefaultNode(template.getId(), 5, "manager_balance", "项目经理平料",
            List.of(task("manager_adjust", "录入材料价格并平料", "manager_adjust", true, 1, null, null)));
        createDefaultNode(template.getId(), 6, "export_text", "导出文本",
            List.of(task("export_run", "导出文本", "plugin_run", true, 1, null, "telecom-export-text-mock")));
    }

    private void createDefaultNode(Long templateId, int sortOrder, String nodeCode, String nodeName, List<DocWorkflowNodeTask> tasks) {
        DocWorkflowTemplateNode node = new DocWorkflowTemplateNode();
        node.setTemplateId(templateId);
        node.setNodeCode(nodeCode);
        node.setNodeName(nodeName);
        node.setSortOrder(sortOrder);
        node.setStatus("active");
        nodeMapper.insert(node);
        for (DocWorkflowNodeTask task : tasks) {
            task.setNodeId(node.getId());
            task.setStatus("active");
            taskMapper.insert(task);
        }
    }

    private DocWorkflowNodeTask task(String code, String name, String type, boolean required, int sortOrder,
                                     String completionRule, String pluginCodes) {
        DocWorkflowNodeTask task = new DocWorkflowNodeTask();
        task.setTaskCode(code);
        task.setTaskName(name);
        task.setTaskType(type);
        task.setRequiredFlag(required);
        task.setSortOrder(sortOrder);
        task.setCompletionRule(completionRule);
        task.setPluginCodes(pluginCodes);
        return task;
    }

    private void initTaskRuntime(Long projectId, String nodeCode) {
        taskRuntimeMapper.delete(new LambdaQueryWrapper<DocProjectNodeTaskRuntime>()
            .eq(DocProjectNodeTaskRuntime::getProjectId, projectId)
            .eq(DocProjectNodeTaskRuntime::getNodeCode, nodeCode));
        DocProjectRuntime runtime = runtimeMapper.selectOne(new LambdaQueryWrapper<DocProjectRuntime>()
            .eq(DocProjectRuntime::getProjectId, projectId));
        List<DocWorkflowTemplateNode> nodes = nodeMapper.selectList(new LambdaQueryWrapper<DocWorkflowTemplateNode>()
            .eq(DocWorkflowTemplateNode::getTemplateId, runtime.getWorkflowTemplateId())
            .eq(DocWorkflowTemplateNode::getNodeCode, nodeCode));
        if (nodes.isEmpty()) {
            return;
        }
        Long nodeId = nodes.get(0).getId();
        List<DocWorkflowNodeTask> tasks = taskMapper.selectList(new LambdaQueryWrapper<DocWorkflowNodeTask>()
            .eq(DocWorkflowNodeTask::getNodeId, nodeId)
            .orderByAsc(DocWorkflowNodeTask::getSortOrder)
            .orderByAsc(DocWorkflowNodeTask::getCreateTime));
        for (DocWorkflowNodeTask task : tasks) {
            DocProjectNodeTaskRuntime taskRuntime = new DocProjectNodeTaskRuntime();
            taskRuntime.setProjectId(projectId);
            taskRuntime.setNodeCode(nodeCode);
            taskRuntime.setTaskCode(task.getTaskCode());
            taskRuntime.setStatus(DocProjectNodeTaskStatus.PENDING.getCode());
            taskRuntimeMapper.insert(taskRuntime);
        }
    }

    private void syncNodeTaskStatuses(DocProject project, DocProjectRuntime runtime, String nodeCode) {
        Map<String, DocWorkflowNodeTask> definitions = listTaskDefinitions(runtime.getWorkflowTemplateId(), nodeCode)
            .stream()
            .collect(HashMap::new, (map, item) -> map.put(item.getTaskCode(), item), HashMap::putAll);
        List<DocProjectNodeTaskRuntime> runtimes = taskRuntimeMapper.selectList(new LambdaQueryWrapper<DocProjectNodeTaskRuntime>()
            .eq(DocProjectNodeTaskRuntime::getProjectId, project.getId())
            .eq(DocProjectNodeTaskRuntime::getNodeCode, nodeCode));
        for (DocProjectNodeTaskRuntime taskRuntime : runtimes) {
            DocWorkflowNodeTask definition = definitions.get(taskRuntime.getTaskCode());
            if (definition == null) {
                continue;
            }
            TaskCompletionEvaluation evaluation = evaluateTaskCompletion(project, definition);
            if (!evaluation.autoManaged()) {
                continue;
            }
            updateTaskRuntimeStatus(taskRuntime, evaluation);
        }
    }

    private void updateTaskRuntimeStatus(DocProjectNodeTaskRuntime taskRuntime, TaskCompletionEvaluation evaluation) {
        if (evaluation.completed()) {
            if (DocProjectNodeTaskStatus.COMPLETED.getCode().equals(taskRuntime.getStatus())
                && StringUtils.equals(taskRuntime.getEvidenceRef(), evaluation.evidenceRef())) {
                return;
            }
            taskRuntime.setStatus(DocProjectNodeTaskStatus.COMPLETED.getCode());
            taskRuntime.setCompletedAt(new Date());
            taskRuntime.setEvidenceRef(evaluation.evidenceRef());
            taskRuntime.setCompletedBy(null);
            taskRuntimeMapper.updateById(taskRuntime);
            return;
        }
        if (DocProjectNodeTaskStatus.COMPLETED.getCode().equals(taskRuntime.getStatus())
            && isAutoEvidence(taskRuntime.getEvidenceRef())) {
            taskRuntime.setStatus(DocProjectNodeTaskStatus.PENDING.getCode());
            taskRuntime.setCompletedAt(null);
            taskRuntime.setCompletedBy(null);
            taskRuntime.setEvidenceRef(null);
            taskRuntimeMapper.updateById(taskRuntime);
        }
    }

    private TaskCompletionEvaluation evaluateTaskCompletion(DocProject project, DocWorkflowNodeTask definition) {
        DocWorkflowTaskCompletionRule rule = resolveCompletionRule(definition);
        if (rule == null) {
            return TaskCompletionEvaluation.manual();
        }
        return switch (rule) {
            case PROJECT_BASIC_INFO_PRESENT -> StringUtils.isNotBlank(project.getName())
                && StringUtils.isNotBlank(project.getCustomerType())
                && StringUtils.isNotBlank(project.getBusinessType())
                ? TaskCompletionEvaluation.autoCompleted(AUTO_EVIDENCE_PREFIX + rule.getCode())
                : TaskCompletionEvaluation.autoPending();
            case DRAWING_EXISTS -> hasIncludedDrawing(project.getId())
                ? TaskCompletionEvaluation.autoCompleted(AUTO_EVIDENCE_PREFIX + rule.getCode())
                : TaskCompletionEvaluation.autoPending();
            case VISA_EXISTS -> hasIncludedVisa(project.getId())
                ? TaskCompletionEvaluation.autoCompleted(AUTO_EVIDENCE_PREFIX + rule.getCode())
                : TaskCompletionEvaluation.autoPending();
            case ESTIMATE_SNAPSHOT_EXISTS -> {
                DocProjectEstimateSnapshotVo snapshot = queryLatestEstimateSnapshot(project.getId());
                yield snapshot != null
                    ? TaskCompletionEvaluation.autoCompleted(AUTO_EVIDENCE_PREFIX + rule.getCode() + ":" + snapshot.getId())
                    : TaskCompletionEvaluation.autoPending();
            }
        };
    }

    private boolean hasIncludedDrawing(Long projectId) {
        return drawingMapper.selectCount(new LambdaQueryWrapper<DocProjectDrawing>()
            .eq(DocProjectDrawing::getProjectId, projectId)
            .eq(DocProjectDrawing::getIncludeInProject, true)) > 0;
    }

    private boolean hasIncludedVisa(Long projectId) {
        return visaMapper.selectCount(new LambdaQueryWrapper<DocProjectVisa>()
            .eq(DocProjectVisa::getProjectId, projectId)
            .eq(DocProjectVisa::getIncludeInProject, true)) > 0;
    }

    private DocProjectEstimateSnapshotVo queryLatestEstimateSnapshot(Long projectId) {
        return estimateSnapshotMapper.selectVoOne(new LambdaQueryWrapper<DocProjectEstimateSnapshot>()
            .eq(DocProjectEstimateSnapshot::getProjectId, projectId)
            .eq(DocProjectEstimateSnapshot::getEstimateType, INITIAL_ESTIMATE_TYPE)
            .orderByDesc(DocProjectEstimateSnapshot::getCreateTime)
            .last("limit 1"));
    }

    private void saveLatestEstimateSnapshot(Long projectId, Long runtimeId) {
        var reader = nodeContextService.buildReader(runtimeId);
        Object estimateAmount = reader.getProcessVariable("estimateAmount");
        if (estimateAmount == null) {
            return;
        }
        DocProjectEstimateSnapshot snapshot = new DocProjectEstimateSnapshot();
        snapshot.setProjectId(projectId);
        snapshot.setEstimateType(INITIAL_ESTIMATE_TYPE);
        snapshot.setEstimateAmount(new java.math.BigDecimal(String.valueOf(estimateAmount)));
        snapshot.setDrawingCount(drawingMapper.selectCount(new LambdaQueryWrapper<DocProjectDrawing>()
            .eq(DocProjectDrawing::getProjectId, projectId)
            .eq(DocProjectDrawing::getIncludeInProject, true)));
        snapshot.setVisaCount(visaMapper.selectCount(new LambdaQueryWrapper<DocProjectVisa>()
            .eq(DocProjectVisa::getProjectId, projectId)
            .eq(DocProjectVisa::getIncludeInProject, true)));
        snapshot.setStatus(String.valueOf(reader.getProcessVariable("estimateStatus")));
        Object summary = reader.getUnstructuredContent("initial_estimate", "estimateSummary");
        snapshot.setSummary(summary == null ? null : String.valueOf(summary));
        estimateSnapshotMapper.delete(new LambdaQueryWrapper<DocProjectEstimateSnapshot>()
            .eq(DocProjectEstimateSnapshot::getProjectId, projectId)
            .eq(DocProjectEstimateSnapshot::getEstimateType, INITIAL_ESTIMATE_TYPE));
        estimateSnapshotMapper.insert(snapshot);
    }

    private void markTaskCompleted(DocProjectNodeTaskRuntime runtime, String evidenceRef) {
        runtime.setStatus(DocProjectNodeTaskStatus.COMPLETED.getCode());
        runtime.setCompletedBy(LoginHelper.getUserId());
        runtime.setCompletedAt(new Date());
        runtime.setEvidenceRef(evidenceRef);
        taskRuntimeMapper.updateById(runtime);
    }

    private void assertCurrentNodeTask(DocProjectRuntime projectRuntime, DocProjectNodeTaskRuntime taskRuntime) {
        if (!StringUtils.equals(projectRuntime.getCurrentNodeCode(), taskRuntime.getNodeCode())) {
            throw new ServiceException("仅允许处理当前节点事项");
        }
    }

    private DocWorkflowNodeTask requiredTaskDefinition(Long templateId, String nodeCode, String taskCode) {
        Long nodeId = resolveNodeId(templateId, nodeCode);
        DocWorkflowNodeTask definition = taskMapper.selectOne(new LambdaQueryWrapper<DocWorkflowNodeTask>()
            .eq(DocWorkflowNodeTask::getNodeId, nodeId)
            .eq(DocWorkflowNodeTask::getTaskCode, taskCode));
        if (definition == null) {
            throw new ServiceException("节点事项定义不存在");
        }
        return definition;
    }

    private List<DocWorkflowNodeTask> listTaskDefinitions(Long templateId, String nodeCode) {
        Long nodeId = resolveNodeId(templateId, nodeCode);
        return taskMapper.selectList(new LambdaQueryWrapper<DocWorkflowNodeTask>()
            .eq(DocWorkflowNodeTask::getNodeId, nodeId)
            .orderByAsc(DocWorkflowNodeTask::getSortOrder)
            .orderByAsc(DocWorkflowNodeTask::getCreateTime));
    }

    private boolean isRequiredTask(DocWorkflowNodeTask definition) {
        return definition == null || Boolean.TRUE.equals(definition.getRequiredFlag());
    }

    private boolean isFinishedTaskStatus(String status) {
        return DocProjectNodeTaskStatus.COMPLETED.getCode().equals(status)
            || DocProjectNodeTaskStatus.SKIPPED.getCode().equals(status);
    }

    private boolean isPluginTask(DocWorkflowNodeTask definition) {
        return StringUtils.equals("plugin_run", definition.getTaskType());
    }

    private boolean usesEstimateSnapshotRule(DocWorkflowNodeTask definition) {
        return resolveCompletionRule(definition) == DocWorkflowTaskCompletionRule.ESTIMATE_SNAPSHOT_EXISTS;
    }

    private DocWorkflowTaskCompletionRule resolveCompletionRule(DocWorkflowNodeTask definition) {
        if (StringUtils.isNotBlank(definition.getCompletionRule())) {
            return DocWorkflowTaskCompletionRule.of(definition.getCompletionRule());
        }
        return switch (definition.getTaskCode()) {
            case "project_info_fill" -> DocWorkflowTaskCompletionRule.PROJECT_BASIC_INFO_PRESENT;
            case "drawing_fill" -> DocWorkflowTaskCompletionRule.DRAWING_EXISTS;
            case "visa_fill" -> DocWorkflowTaskCompletionRule.VISA_EXISTS;
            case "estimate_run" -> DocWorkflowTaskCompletionRule.ESTIMATE_SNAPSHOT_EXISTS;
            default -> null;
        };
    }

    private List<String> parsePluginCodes(String pluginCodes) {
        List<String> results = new ArrayList<>();
        for (String pluginCode : pluginCodes.split(",")) {
            String trimmed = pluginCode.trim();
            if (!trimmed.isEmpty()) {
                results.add(trimmed);
            }
        }
        return results;
    }

    private String resolveNasBasePath(DocProject project) {
        return project.getNasBasePath() == null ? ("/docman/project/" + project.getId()) : project.getNasBasePath();
    }

    private boolean isAutoEvidence(String evidenceRef) {
        return StringUtils.isNotBlank(evidenceRef) && evidenceRef.startsWith(AUTO_EVIDENCE_PREFIX);
    }

    private record TaskCompletionEvaluation(boolean autoManaged, boolean completed, String evidenceRef) {

        private static TaskCompletionEvaluation manual() {
            return new TaskCompletionEvaluation(false, false, null);
        }

        private static TaskCompletionEvaluation autoPending() {
            return new TaskCompletionEvaluation(true, false, null);
        }

        private static TaskCompletionEvaluation autoCompleted(String evidenceRef) {
            return new TaskCompletionEvaluation(true, true, evidenceRef);
        }
    }

    private List<DocWorkflowTemplateNodeVo> buildNodeVos(List<DocWorkflowTemplateNode> nodes) {
        List<DocWorkflowTemplateNodeVo> result = new ArrayList<>();
        for (DocWorkflowTemplateNode node : nodes) {
            DocWorkflowTemplateNodeVo vo = new DocWorkflowTemplateNodeVo();
            vo.setId(node.getId());
            vo.setTemplateId(node.getTemplateId());
            vo.setNodeCode(node.getNodeCode());
            vo.setNodeName(node.getNodeName());
            vo.setSortOrder(node.getSortOrder());
            vo.setDescription(node.getDescription());
            vo.setStatus(node.getStatus());
            vo.setTasks(buildTaskDefs(node.getId()));
            result.add(vo);
        }
        return result;
    }

    private List<DocWorkflowNodeTaskVo> buildTaskDefs(Long nodeId) {
        List<DocWorkflowNodeTaskVo> taskVos = new ArrayList<>();
        List<DocWorkflowNodeTask> tasks = taskMapper.selectList(new LambdaQueryWrapper<DocWorkflowNodeTask>()
            .eq(DocWorkflowNodeTask::getNodeId, nodeId)
            .orderByAsc(DocWorkflowNodeTask::getSortOrder)
            .orderByAsc(DocWorkflowNodeTask::getCreateTime));
        for (DocWorkflowNodeTask task : tasks) {
            DocWorkflowNodeTaskVo vo = new DocWorkflowNodeTaskVo();
            vo.setId(task.getId());
            vo.setNodeId(task.getNodeId());
            vo.setTaskCode(task.getTaskCode());
            vo.setTaskName(task.getTaskName());
            vo.setTaskType(task.getTaskType());
            vo.setRequiredFlag(task.getRequiredFlag());
            vo.setSortOrder(task.getSortOrder());
            vo.setCompletionRule(task.getCompletionRule());
            vo.setPluginCodes(task.getPluginCodes());
            vo.setDescription(task.getDescription());
            vo.setStatus(task.getStatus());
            taskVos.add(vo);
        }
        return taskVos;
    }

    private List<DocProjectNodeTaskRuntimeVo> buildTaskRuntimeVos(Long projectId, String nodeCode) {
        List<DocProjectNodeTaskRuntimeVo> result = new ArrayList<>();
        List<DocProjectNodeTaskRuntime> runtimes = taskRuntimeMapper.selectList(new LambdaQueryWrapper<DocProjectNodeTaskRuntime>()
            .eq(DocProjectNodeTaskRuntime::getProjectId, projectId)
            .eq(DocProjectNodeTaskRuntime::getNodeCode, nodeCode)
            .orderByAsc(DocProjectNodeTaskRuntime::getCreateTime));
        DocProjectRuntime runtime = runtimeMapper.selectOne(new LambdaQueryWrapper<DocProjectRuntime>()
            .eq(DocProjectRuntime::getProjectId, projectId));
        List<DocWorkflowTemplateNode> nodes = nodeMapper.selectList(new LambdaQueryWrapper<DocWorkflowTemplateNode>()
            .eq(DocWorkflowTemplateNode::getTemplateId, runtime.getWorkflowTemplateId())
            .eq(DocWorkflowTemplateNode::getNodeCode, nodeCode));
        Long nodeId = nodes.isEmpty() ? null : nodes.get(0).getId();
        List<DocWorkflowNodeTask> definitions = nodeId == null ? List.of() : taskMapper.selectList(new LambdaQueryWrapper<DocWorkflowNodeTask>()
            .eq(DocWorkflowNodeTask::getNodeId, nodeId));
        for (DocProjectNodeTaskRuntime item : runtimes) {
            DocProjectNodeTaskRuntimeVo vo = new DocProjectNodeTaskRuntimeVo();
            vo.setId(item.getId());
            vo.setProjectId(item.getProjectId());
            vo.setNodeCode(item.getNodeCode());
            vo.setTaskCode(item.getTaskCode());
            vo.setStatus(item.getStatus());
            vo.setCompletedBy(item.getCompletedBy());
            vo.setCompletedAt(item.getCompletedAt());
            vo.setEvidenceRef(item.getEvidenceRef());
            for (DocWorkflowNodeTask definition : definitions) {
                if (definition.getTaskCode().equals(item.getTaskCode())) {
                    vo.setTaskName(definition.getTaskName());
                    vo.setTaskType(definition.getTaskType());
                    vo.setRequiredFlag(definition.getRequiredFlag());
                    vo.setSortOrder(definition.getSortOrder());
                    vo.setCompletionRule(definition.getCompletionRule());
                    vo.setPluginCodes(definition.getPluginCodes());
                    break;
                }
            }
            result.add(vo);
        }
        result.sort((a, b) -> Integer.compare(a.getSortOrder() == null ? 0 : a.getSortOrder(), b.getSortOrder() == null ? 0 : b.getSortOrder()));
        return result;
    }

    private String resolveNodeName(List<DocWorkflowTemplateNode> nodes, String currentNodeCode) {
        return nodes.stream()
            .filter(node -> node.getNodeCode().equals(currentNodeCode))
            .map(DocWorkflowTemplateNode::getNodeName)
            .findFirst()
            .orElse(currentNodeCode);
    }

    private String resolveNextNodeCode(List<DocWorkflowTemplateNode> nodes, String currentNodeCode) {
        for (int i = 0; i < nodes.size(); i++) {
            if (nodes.get(i).getNodeCode().equals(currentNodeCode)) {
                return i + 1 < nodes.size() ? nodes.get(i + 1).getNodeCode() : null;
            }
        }
        return null;
    }

    private Long resolveNodeId(Long templateId, String nodeCode) {
        return nodeMapper.selectList(new LambdaQueryWrapper<DocWorkflowTemplateNode>()
                .eq(DocWorkflowTemplateNode::getTemplateId, templateId)
                .eq(DocWorkflowTemplateNode::getNodeCode, nodeCode))
            .stream()
            .findFirst()
            .map(DocWorkflowTemplateNode::getId)
            .orElseThrow(() -> new ServiceException("流程节点不存在: " + nodeCode));
    }
}
