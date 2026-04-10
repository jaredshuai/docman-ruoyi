package org.dromara.docman.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.docman.application.service.DocWorkflowNodeApplicationService;
import org.dromara.docman.context.NodeContextReader;
import org.dromara.docman.domain.bo.DocProjectAdvanceNodeBo;
import org.dromara.docman.domain.bo.DocProjectNodeTaskCompleteBo;
import org.dromara.docman.domain.entity.DocNodeContext;
import org.dromara.docman.domain.entity.DocDocumentRecord;
import org.dromara.docman.domain.entity.DocProject;
import org.dromara.docman.domain.entity.DocProjectDrawing;
import org.dromara.docman.domain.entity.DocProjectEstimateSnapshot;
import org.dromara.docman.domain.entity.DocProjectNodeTaskRuntime;
import org.dromara.docman.domain.entity.DocProjectRuntime;
import org.dromara.docman.domain.entity.DocProjectVisa;
import org.dromara.docman.domain.entity.DocWorkflowNodeTask;
import org.dromara.docman.domain.entity.DocWorkflowTemplate;
import org.dromara.docman.domain.entity.DocWorkflowTemplateNode;
import org.dromara.docman.domain.enums.DocProjectAction;
import org.dromara.docman.domain.vo.DocProjectEstimateSnapshotVo;
import org.dromara.docman.mapper.DocDocumentRecordMapper;
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
import org.dromara.docman.plugin.PluginResult;
import org.dromara.docman.plugin.runtime.PluginExecutionResult;
import org.dromara.docman.service.IDocProjectAccessService;
import org.dromara.docman.service.INodeContextService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("dev")
@Tag("prod")
@Tag("local")
class DocProjectWorkspaceServiceImplTest {

    @BeforeAll
    static void initTableInfo() {
        initTableInfo(DocProject.class);
        initTableInfo(DocDocumentRecord.class);
        initTableInfo(DocProjectDrawing.class);
        initTableInfo(DocProjectEstimateSnapshot.class);
        initTableInfo(DocProjectNodeTaskRuntime.class);
        initTableInfo(DocProjectRuntime.class);
        initTableInfo(DocProjectVisa.class);
        initTableInfo(DocWorkflowNodeTask.class);
        initTableInfo(DocWorkflowTemplate.class);
        initTableInfo(DocWorkflowTemplateNode.class);
    }

    @Mock private IDocProjectAccessService projectAccessService;
    @Mock private DocProjectMapper projectMapper;
    @Mock private DocProjectRuntimeMapper runtimeMapper;
    @Mock private DocProjectTypeMapper projectTypeMapper;
    @Mock private DocWorkflowTemplateMapper templateMapper;
    @Mock private DocWorkflowTemplateNodeMapper nodeMapper;
    @Mock private DocWorkflowNodeTaskMapper taskMapper;
    @Mock private DocProjectNodeTaskRuntimeMapper taskRuntimeMapper;
    @Mock private DocProjectDrawingMapper drawingMapper;
    @Mock private DocDocumentRecordMapper documentRecordMapper;
    @Mock private DocProjectVisaMapper visaMapper;
    @Mock private DocProjectEstimateSnapshotMapper estimateSnapshotMapper;
    @Mock private INodeContextService nodeContextService;
    @Mock private DocWorkflowNodeApplicationService workflowNodeApplicationService;

    @InjectMocks
    private DocProjectWorkspaceServiceImpl service;

    @Test
    void shouldRejectAdvanceWhenCurrentTasksIncomplete() {
        Long projectId = 1L;
        DocProject project = project(projectId);
        DocProjectRuntime runtime = runtime(projectId, 10L, "drawing_input");
        DocWorkflowNodeTask definition = task("drawing_fill", "form_fill", true, "drawing_exists", null);

        DocProjectAdvanceNodeBo bo = new DocProjectAdvanceNodeBo();
        bo.setProjectId(projectId);
        bo.setCurrentNodeCode("drawing_input");

        DocProjectNodeTaskRuntime taskRuntime = new DocProjectNodeTaskRuntime();
        taskRuntime.setProjectId(projectId);
        taskRuntime.setNodeCode("drawing_input");
        taskRuntime.setTaskCode("drawing_fill");
        taskRuntime.setStatus("pending");

        when(projectMapper.selectById(projectId)).thenReturn(project);
        when(runtimeMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(runtime);
        when(nodeMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(node(10L, "drawing_input")));
        when(taskMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(definition));
        when(taskRuntimeMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(taskRuntime));
        when(drawingMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);

        ServiceException ex = assertThrows(ServiceException.class, () -> service.advanceNode(bo));

        assertEquals("当前节点仍有未完成事项，无法推进", ex.getMessage());
        verify(runtimeMapper, never()).updateById(any(DocProjectRuntime.class));
    }

    @Test
    void shouldCompleteTaskWithEvidenceAndOperator() {
        Long projectId = 1L;
        Long taskRuntimeId = 2L;
        DocProject project = project(projectId);
        DocProjectRuntime projectRuntime = runtime(projectId, 10L, "project_info");
        DocProjectNodeTaskRuntime runtime = new DocProjectNodeTaskRuntime();
        runtime.setId(taskRuntimeId);
        runtime.setProjectId(projectId);
        runtime.setNodeCode("project_info");
        runtime.setTaskCode("project_info_confirm");
        runtime.setStatus("pending");

        DocProjectNodeTaskCompleteBo bo = new DocProjectNodeTaskCompleteBo();
        bo.setEvidenceRef("confirm:1");

        when(projectMapper.selectById(projectId)).thenReturn(project);
        when(runtimeMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(projectRuntime);
        when(taskRuntimeMapper.selectById(taskRuntimeId)).thenReturn(runtime);
        when(nodeMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(node(10L, "project_info")));
        when(taskMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(task("project_info_confirm", "review_confirm", true, null, null));

        try (MockedStatic<LoginHelper> loginHelper = mockStatic(LoginHelper.class)) {
            loginHelper.when(LoginHelper::getUserId).thenReturn(99L);
            service.completeTask(projectId, taskRuntimeId, bo);
        }

        assertEquals("completed", runtime.getStatus());
        assertEquals(99L, runtime.getCompletedBy());
        assertEquals("confirm:1", runtime.getEvidenceRef());
        verify(taskRuntimeMapper).updateById(runtime);
    }

    @Test
    void shouldAutoCompleteDrawingTaskWhenIncludedDrawingExists() {
        Long projectId = 1L;
        DocProject project = project(projectId);
        DocProjectRuntime runtime = runtime(projectId, 10L, "drawing_input");
        DocWorkflowTemplate template = new DocWorkflowTemplate();
        template.setId(10L);
        DocWorkflowTemplateNode node = node(10L, "drawing_input");
        DocWorkflowNodeTask definition = task("drawing_fill", "form_fill", true, "drawing_exists", null);
        DocProjectNodeTaskRuntime taskRuntime = new DocProjectNodeTaskRuntime();
        taskRuntime.setId(3L);
        taskRuntime.setProjectId(projectId);
        taskRuntime.setNodeCode("drawing_input");
        taskRuntime.setTaskCode("drawing_fill");
        taskRuntime.setStatus("pending");

        when(projectMapper.selectById(projectId)).thenReturn(project);
        when(runtimeMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(runtime);
        when(templateMapper.selectById(10L)).thenReturn(template);
        when(nodeMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(node), List.of(node), List.of(node));
        when(taskMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(definition), List.of(definition), List.of(definition));
        when(taskRuntimeMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(taskRuntime));
        when(drawingMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L, 1L);
        when(visaMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(estimateSnapshotMapper.selectVoOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        var workspace = service.getWorkspace(projectId);

        assertEquals("completed", workspace.getCurrentNodeTasks().get(0).getStatus());
        assertEquals(1L, workspace.getDrawingCount());
        assertEquals(1L, workspace.getIncludedDrawingCount());
        assertEquals(0L, workspace.getVisaCount());
        assertEquals(0L, workspace.getIncludedVisaCount());
        assertEquals("completed", taskRuntime.getStatus());
        verify(taskRuntimeMapper).updateById(taskRuntime);
    }

    @Test
    void shouldExposeLatestExportArtifactFromWorkspaceAggregation() {
        Long projectId = 1L;
        DocProject project = project(projectId);
        DocProjectRuntime runtime = runtime(projectId, 10L, "export_text");
        DocWorkflowTemplate template = new DocWorkflowTemplate();
        template.setId(10L);
        DocWorkflowTemplateNode exportNode = node(10L, "export_text");
        DocWorkflowNodeTask exportTask = task("export_run", "plugin_run", true, null, "telecom-export-text-v2");
        DocProjectNodeTaskRuntime taskRuntime = new DocProjectNodeTaskRuntime();
        taskRuntime.setId(30L);
        taskRuntime.setProjectId(projectId);
        taskRuntime.setNodeCode("export_text");
        taskRuntime.setTaskCode("export_run");
        taskRuntime.setStatus("pending");
        DocDocumentRecord latestArtifact = new DocDocumentRecord();
        latestArtifact.setId(901L);
        latestArtifact.setProjectId(projectId);
        latestArtifact.setPluginId("telecom-export-text-v2");
        latestArtifact.setSourceType("plugin");
        latestArtifact.setFileName("latest.txt");
        latestArtifact.setNasPath("/docman/project/1/export/latest.txt");
        latestArtifact.setStatus("obsolete");

        when(projectMapper.selectById(projectId)).thenReturn(project);
        when(runtimeMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(runtime);
        when(templateMapper.selectById(10L)).thenReturn(template);
        when(nodeMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(
            List.of(exportNode),
            List.of(exportNode),
            List.of(exportNode)
        );
        when(taskMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(
            List.of(exportTask),
            List.of(exportTask),
            List.of(exportTask)
        );
        when(taskRuntimeMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(taskRuntime));
        when(drawingMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(visaMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(estimateSnapshotMapper.selectVoOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        when(documentRecordMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(latestArtifact);

        var workspace = service.getWorkspace(projectId);

        assertNotNull(workspace.getLatestExportArtifact());
        assertEquals(901L, workspace.getLatestExportArtifact().getId());
        assertEquals("obsolete", workspace.getLatestExportArtifact().getStatus());
        assertEquals("latest.txt", workspace.getLatestExportArtifact().getFileName());
    }

    @Test
    void shouldPersistEstimateSnapshotWhenTriggerEstimate() {
        Long projectId = 1L;
        Long runtimeId = 100L;
        DocProject project = project(projectId);
        DocProjectRuntime runtime = runtime(projectId, 10L, "initial_estimate");
        DocWorkflowTemplateNode node = node(10L, "initial_estimate");
        DocWorkflowNodeTask definition = task("estimate_run", "plugin_run", true, "estimate_snapshot_exists", "telecom-estimate-mock");
        DocProjectNodeTaskRuntime taskRuntime = new DocProjectNodeTaskRuntime();
        taskRuntime.setId(8L);
        taskRuntime.setProjectId(projectId);
        taskRuntime.setNodeCode("initial_estimate");
        taskRuntime.setTaskCode("estimate_run");
        taskRuntime.setStatus("pending");
        DocProjectEstimateSnapshotVo snapshotVo = new DocProjectEstimateSnapshotVo();
        snapshotVo.setId(99L);
        snapshotVo.setProjectId(projectId);
        snapshotVo.setEstimateAmount(new BigDecimal("1234"));

        NodeContextReader reader = mock(NodeContextReader.class);
        DocNodeContext nodeContext = mock(DocNodeContext.class);

        when(projectMapper.selectById(projectId)).thenReturn(project);
        when(runtimeMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(runtime);
        when(nodeMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(node), List.of(node), List.of(node), List.of(node));
        when(taskMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(definition), List.of(definition));
        when(taskMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(definition, definition);
        when(taskRuntimeMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(taskRuntime), List.of(taskRuntime));
        when(taskRuntimeMapper.selectById(8L)).thenReturn(taskRuntime);
        when(drawingMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(3L, 3L);
        when(visaMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L, 1L);
        when(estimateSnapshotMapper.selectVoOne(any(LambdaQueryWrapper.class))).thenReturn(null, snapshotVo);
        when(nodeContextService.getOrCreate(runtimeId, "initial_estimate", projectId)).thenReturn(nodeContext);
        when(nodeContext.getId()).thenReturn(66L);
        when(nodeContextService.buildReader(runtimeId)).thenReturn(reader);
        when(reader.getProcessVariable("estimateAmount")).thenReturn(new BigDecimal("1234"));
        when(reader.getProcessVariable("estimateStatus")).thenReturn("mocked");
        when(reader.getUnstructuredContent("initial_estimate", "estimateSummary")).thenReturn("summary");
        when(workflowNodeApplicationService.triggerBoundPlugins(any(), any(), any(), any(), any(), any(), any()))
            .thenReturn(List.of(PluginExecutionResult.builder()
                .pluginId("telecom-estimate-mock")
                .costMs(10L)
                .result(PluginResult.ok())
                .build()));

        service.triggerEstimate(projectId);

        verify(estimateSnapshotMapper).delete(any(LambdaQueryWrapper.class));
        verify(estimateSnapshotMapper).insert(any(DocProjectEstimateSnapshot.class));
        verify(taskRuntimeMapper, times(1)).updateById(taskRuntime);
        assertEquals("completed", taskRuntime.getStatus());
    }

    @Test
    void shouldRejectTriggerEstimateBeforeLoadingProjectWhenNoProjectAccess() {
        Long projectId = 1L;
        doThrow(new ServiceException("你无权访问该项目"))
            .when(projectAccessService).assertAction(projectId, DocProjectAction.EDIT_PROJECT);

        ServiceException ex = assertThrows(ServiceException.class, () -> service.triggerEstimate(projectId));

        assertEquals("你无权访问该项目", ex.getMessage());
        verify(projectMapper, never()).selectById(projectId);
    }

    @Test
    void shouldRejectExportWhenEstimateSnapshotMissing() {
        Long projectId = 1L;
        DocProject project = project(projectId);
        DocProjectRuntime runtime = runtime(projectId, 10L, "export_text");
        DocWorkflowNodeTask definition = task("export_run", "plugin_run", true, null, "telecom-export-text-mock");
        DocProjectNodeTaskRuntime taskRuntime = new DocProjectNodeTaskRuntime();
        taskRuntime.setId(18L);
        taskRuntime.setProjectId(projectId);
        taskRuntime.setNodeCode("export_text");
        taskRuntime.setTaskCode("export_run");
        taskRuntime.setStatus("pending");

        when(projectMapper.selectById(projectId)).thenReturn(project);
        when(runtimeMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(runtime);
        when(nodeMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(node(10L, "export_text")));
        when(taskMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(definition));
        when(taskRuntimeMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(taskRuntime));
        when(estimateSnapshotMapper.selectVoOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class, () -> service.triggerExportText(projectId));

        assertEquals("请先完成初步估算后再导出文本", ex.getMessage());
        verify(projectAccessService).assertAction(projectId, DocProjectAction.EDIT_PROJECT);
        verify(workflowNodeApplicationService, never()).triggerBoundPlugins(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void shouldRejectTriggerExportBeforeLoadingProjectWhenNoProjectAccess() {
        Long projectId = 1L;
        doThrow(new ServiceException("你无权访问该项目"))
            .when(projectAccessService).assertAction(projectId, DocProjectAction.EDIT_PROJECT);

        ServiceException ex = assertThrows(ServiceException.class, () -> service.triggerExportText(projectId));

        assertEquals("你无权访问该项目", ex.getMessage());
        verify(projectMapper, never()).selectById(projectId);
    }

    @Test
    void shouldTriggerExportWhenConfiguredPluginBindingChanges() {
        Long projectId = 1L;
        Long runtimeId = 100L;
        DocProject project = project(projectId);
        DocProjectRuntime runtime = runtime(projectId, 10L, "export_text");
        DocWorkflowTemplateNode node = node(10L, "export_text");
        DocWorkflowNodeTask definition = task("export_run", "plugin_run", true, null, "telecom-export-text-v2");
        DocProjectNodeTaskRuntime taskRuntime = new DocProjectNodeTaskRuntime();
        taskRuntime.setId(19L);
        taskRuntime.setProjectId(projectId);
        taskRuntime.setNodeCode("export_text");
        taskRuntime.setTaskCode("export_run");
        taskRuntime.setStatus("pending");
        DocProjectEstimateSnapshotVo snapshotVo = new DocProjectEstimateSnapshotVo();
        snapshotVo.setId(101L);
        snapshotVo.setProjectId(projectId);

        DocNodeContext nodeContext = mock(DocNodeContext.class);

        when(projectMapper.selectById(projectId)).thenReturn(project);
        when(runtimeMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(runtime);
        when(nodeMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(node), List.of(node), List.of(node));
        when(taskMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(definition));
        when(taskMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(definition, definition);
        when(taskRuntimeMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(taskRuntime), List.of(taskRuntime));
        when(estimateSnapshotMapper.selectVoOne(any(LambdaQueryWrapper.class))).thenReturn(snapshotVo);
        when(nodeContextService.getOrCreate(runtimeId, "export_text", projectId)).thenReturn(nodeContext);
        when(nodeContext.getId()).thenReturn(77L);
        when(drawingMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(2L);
        when(visaMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);
        when(workflowNodeApplicationService.triggerBoundPlugins(any(), any(), any(), any(), any(), any(), any()))
            .thenReturn(List.of(PluginExecutionResult.builder()
                .pluginId("telecom-export-text-v2")
                .costMs(12L)
                .result(PluginResult.ok(List.of(PluginResult.GeneratedFile.builder()
                    .fileName("telecom.txt")
                    .nasPath("/docman/project/1/export/telecom.txt")
                    .build())))
                .build()));

        try (MockedStatic<LoginHelper> loginHelper = mockStatic(LoginHelper.class)) {
            loginHelper.when(LoginHelper::getUserId).thenReturn(88L);
            service.triggerExportText(projectId);
        }

        verify(workflowNodeApplicationService).triggerBoundPlugins(any(), any(), any(), any(), any(), any(), any());
        verify(taskRuntimeMapper).updateById(taskRuntime);
        verify(projectAccessService).assertAction(projectId, DocProjectAction.EDIT_PROJECT);
        assertEquals("completed", taskRuntime.getStatus());
    }

    @Test
    void shouldRejectExportWhenPluginSucceedsWithoutGeneratedFiles() {
        Long projectId = 1L;
        DocProject project = project(projectId);
        DocProjectRuntime runtime = runtime(projectId, 10L, "export_text");
        DocWorkflowTemplateNode node = node(10L, "export_text");
        DocWorkflowNodeTask definition = task("export_run", "plugin_run", true, null, "telecom-export-text-v2");
        DocProjectNodeTaskRuntime taskRuntime = new DocProjectNodeTaskRuntime();
        taskRuntime.setId(20L);
        taskRuntime.setProjectId(projectId);
        taskRuntime.setNodeCode("export_text");
        taskRuntime.setTaskCode("export_run");
        taskRuntime.setStatus("pending");
        DocProjectEstimateSnapshotVo snapshotVo = new DocProjectEstimateSnapshotVo();
        snapshotVo.setId(102L);
        snapshotVo.setProjectId(projectId);

        DocNodeContext nodeContext = mock(DocNodeContext.class);

        when(projectMapper.selectById(projectId)).thenReturn(project);
        when(runtimeMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(runtime);
        when(nodeMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(node), List.of(node), List.of(node));
        when(taskMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(definition));
        when(taskMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(definition, definition);
        when(taskRuntimeMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(taskRuntime), List.of(taskRuntime));
        when(estimateSnapshotMapper.selectVoOne(any(LambdaQueryWrapper.class))).thenReturn(snapshotVo);
        when(nodeContextService.getOrCreate(100L, "export_text", projectId)).thenReturn(nodeContext);
        when(nodeContext.getId()).thenReturn(77L);
        when(drawingMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(2L);
        when(visaMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);
        when(workflowNodeApplicationService.triggerBoundPlugins(any(), any(), any(), any(), any(), any(), any()))
            .thenReturn(List.of(PluginExecutionResult.builder()
                .pluginId("telecom-export-text-v2")
                .costMs(12L)
                .result(PluginResult.ok())
                .build()));

        ServiceException ex = assertThrows(ServiceException.class, () -> service.triggerExportText(projectId));

        assertEquals("文本导出未生成有效产物，事项未完成", ex.getMessage());
        verify(taskRuntimeMapper, never()).updateById(taskRuntime);
        assertEquals("pending", taskRuntime.getStatus());
    }

    @Test
    void shouldAllowAdvanceWhenOnlyOptionalTaskPending() {
        Long projectId = 1L;
        DocProject project = project(projectId);
        DocProjectRuntime runtime = runtime(projectId, 10L, "drawing_input");

        DocProjectAdvanceNodeBo bo = new DocProjectAdvanceNodeBo();
        bo.setProjectId(projectId);
        bo.setCurrentNodeCode("drawing_input");

        DocProjectNodeTaskRuntime taskRuntime = new DocProjectNodeTaskRuntime();
        taskRuntime.setProjectId(projectId);
        taskRuntime.setNodeCode("drawing_input");
        taskRuntime.setTaskCode("drawing_optional");
        taskRuntime.setStatus("pending");

        when(projectMapper.selectById(projectId)).thenReturn(project);
        when(runtimeMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(runtime);
        when(nodeMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(
            List.of(node(10L, "drawing_input")),
            List.of(node(10L, "drawing_input"), node(10L, "visa_input"))
        );
        when(taskMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(task("drawing_optional", "form_fill", false, null, null)));
        when(taskRuntimeMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(taskRuntime));

        service.advanceNode(bo);

        assertEquals("visa_input", runtime.getCurrentNodeCode());
        verify(runtimeMapper).updateById(runtime);
    }

    private static DocProject project(Long id) {
        DocProject project = new DocProject();
        project.setId(id);
        project.setName("test");
        project.setCustomerType("telecom");
        project.setBusinessType("pipeline");
        project.setProjectTypeCode("telecom");
        return project;
    }

    private static DocProjectRuntime runtime(Long projectId, Long templateId, String nodeCode) {
        DocProjectRuntime runtime = new DocProjectRuntime();
        runtime.setId(100L);
        runtime.setProjectId(projectId);
        runtime.setWorkflowTemplateId(templateId);
        runtime.setCurrentNodeCode(nodeCode);
        runtime.setStatus("running");
        return runtime;
    }

    private static DocWorkflowTemplateNode node(Long templateId, String nodeCode) {
        DocWorkflowTemplateNode node = new DocWorkflowTemplateNode();
        node.setId(11L);
        node.setTemplateId(templateId);
        node.setNodeCode(nodeCode);
        node.setNodeName(nodeCode);
        node.setSortOrder(1);
        node.setStatus("active");
        return node;
    }

    private static DocWorkflowNodeTask task(String taskCode, String taskType, boolean required,
                                            String completionRule, String pluginCodes) {
        DocWorkflowNodeTask task = new DocWorkflowNodeTask();
        task.setId(21L);
        task.setNodeId(11L);
        task.setTaskCode(taskCode);
        task.setTaskName(taskCode);
        task.setTaskType(taskType);
        task.setRequiredFlag(required);
        task.setSortOrder(1);
        task.setCompletionRule(completionRule);
        task.setPluginCodes(pluginCodes);
        task.setStatus("active");
        return task;
    }

    private static void initTableInfo(Class<?> entityClass) {
        if (TableInfoHelper.getTableInfo(entityClass) == null) {
            TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), "test"), entityClass);
        }
    }
}
