package org.dromara.docman.application.service;

import org.dromara.common.core.domain.event.WorkflowNodeFinishedEvent;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.docman.context.NodeContextReader;
import org.dromara.docman.domain.entity.DocNodeContext;
import org.dromara.docman.domain.entity.DocProcessConfig;
import org.dromara.docman.domain.enums.DocProcessConfigStatus;
import org.dromara.docman.domain.vo.DocProjectVo;
import org.dromara.docman.plugin.DocumentPlugin;
import org.dromara.docman.plugin.PluginRegistry;
import org.dromara.docman.plugin.PluginResult;
import org.dromara.docman.plugin.runtime.PluginExecutionRequest;
import org.dromara.docman.plugin.runtime.PluginExecutionResult;
import org.dromara.docman.plugin.runtime.PluginExecutor;
import org.dromara.docman.service.IDocDocumentRecordService;
import org.dromara.docman.service.IDocProcessConfigService;
import org.dromara.docman.service.IDocProjectService;
import org.dromara.docman.service.INodeContextService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("dev")
@Tag("prod")
@Tag("local")
class DocWorkflowNodeApplicationServiceTest {

    @Mock
    private PluginRegistry pluginRegistry;

    @Mock
    private PluginExecutor pluginExecutor;

    @Mock
    private INodeContextService contextService;

    @Mock
    private IDocProcessConfigService processConfigService;

    @Mock
    private IDocProjectService projectService;

    @Mock
    private IDocDocumentRecordService documentRecordService;

    @Mock
    private NodeContextReader contextReader;

    @InjectMocks
    private DocWorkflowNodeApplicationService service;

    @Test
    void shouldUpdateStatusToCompletedWhenFinishStatusViaHandleNodeFinished() {
        // Setup event with finish status
        WorkflowNodeFinishedEvent event = new WorkflowNodeFinishedEvent();
        event.setInstanceId(100L);
        event.setStatus("finish");
        event.setNodeCode("node1");

        WorkflowNodeFinishedEvent.NodeExtPayload parsedExt = new WorkflowNodeFinishedEvent.NodeExtPayload();
        parsedExt.setPlugins(Collections.emptyList());
        event.setParsedExt(parsedExt);

        // Setup config in RUNNING state
        DocProcessConfig config = new DocProcessConfig();
        config.setId(1L);
        config.setProjectId(10L);
        config.setStatus("running");

        when(processConfigService.queryByInstanceId(100L)).thenReturn(config);

        // Execute
        service.handleNodeFinished(event);

        // Verify status updated to completed
        verify(processConfigService).updateStatus(1L, DocProcessConfigStatus.COMPLETED.getCode());
    }

    @Test
    void shouldNotUpdateStatusWhenNotFinishStatusViaHandleNodeFinished() {
        // Setup event with non-finish status
        WorkflowNodeFinishedEvent event = new WorkflowNodeFinishedEvent();
        event.setInstanceId(100L);
        event.setStatus("waiting");
        event.setNodeCode("node1");

        WorkflowNodeFinishedEvent.NodeExtPayload parsedExt = new WorkflowNodeFinishedEvent.NodeExtPayload();
        parsedExt.setPlugins(Collections.emptyList());
        event.setParsedExt(parsedExt);

        // Execute
        service.handleNodeFinished(event);

        // Verify status not updated
        verify(processConfigService, never()).updateStatus(anyLong(), anyString());
    }

    @Test
    void shouldSkipPluginWhenNotRegistered() {
        // Setup event with plugin binding
        WorkflowNodeFinishedEvent.PluginBinding binding = new WorkflowNodeFinishedEvent.PluginBinding();
        binding.setPluginId("nonexistent-plugin");
        binding.setConfig(Map.of("key", "value"));

        WorkflowNodeFinishedEvent.NodeExtPayload parsedExt = new WorkflowNodeFinishedEvent.NodeExtPayload();
        parsedExt.setPlugins(List.of(binding));

        WorkflowNodeFinishedEvent event = new WorkflowNodeFinishedEvent();
        event.setInstanceId(100L);
        event.setStatus("waiting");
        event.setNodeCode("node1");
        event.setParsedExt(parsedExt);

        // Setup config and project
        DocProcessConfig config = new DocProcessConfig();
        config.setId(1L);
        config.setProjectId(10L);
        config.setStatus("running");

        DocProjectVo project = new DocProjectVo();
        project.setId(10L);
        project.setName("Test Project");

        DocNodeContext nodeContext = DocNodeContext.create(100L, "node1", 10L);
        nodeContext.setId(1000L);

        when(processConfigService.queryByInstanceId(100L)).thenReturn(config);
        when(projectService.queryById(10L)).thenReturn(project);
        when(contextService.getOrCreate(100L, "node1", 10L)).thenReturn(nodeContext);
        when(contextService.buildReader(100L)).thenReturn(contextReader);
        when(pluginRegistry.getPlugin("nonexistent-plugin")).thenReturn(null); // Plugin not registered

        // Execute
        service.handleNodeFinished(event);

        // Verify plugin executor was never called
        verify(pluginExecutor, never()).execute(any());
    }

    @Test
    void shouldThrowExceptionWhenProjectNullInTriggerPlugins() {
        // Setup config
        DocProcessConfig config = new DocProcessConfig();
        config.setId(1L);
        config.setProjectId(10L);
        config.setInstanceId(100L);
        config.setDefinitionId(5L);

        when(projectService.queryById(10L)).thenReturn(null);

        // Execute and verify exception
        ServiceException ex = assertThrows(ServiceException.class,
            () -> service.triggerPlugins(config, null));

        assertEquals("项目不存在: 10", ex.getMessage());
    }

    // ==================== handleNodeFinished Branch Tests ====================

    /**
     * 配置缺失时应在查询项目前返回：不执行插件、不查项目。
     */
    @Test
    void shouldSkipPluginExecutionWhenProcessConfigMissing() {
        WorkflowNodeFinishedEvent.PluginBinding binding = new WorkflowNodeFinishedEvent.PluginBinding();
        binding.setPluginId("test-plugin");

        WorkflowNodeFinishedEvent.NodeExtPayload parsedExt = new WorkflowNodeFinishedEvent.NodeExtPayload();
        parsedExt.setPlugins(List.of(binding));

        WorkflowNodeFinishedEvent event = new WorkflowNodeFinishedEvent();
        event.setInstanceId(100L);
        event.setStatus("waiting");
        event.setNodeCode("node1");
        event.setParsedExt(parsedExt);

        when(processConfigService.queryByInstanceId(100L)).thenReturn(null);

        service.handleNodeFinished(event);

        verify(processConfigService).queryByInstanceId(100L);
        verify(projectService, never()).queryById(anyLong());
        verify(contextService, never()).getOrCreate(anyLong(), anyString(), anyLong());
        verify(pluginExecutor, never()).execute(any());
    }

    /**
     * 项目不存在时不应执行插件（与实现中 project == null 分支一致）。
     */
    @Test
    void shouldSkipPluginExecutionWhenProjectMissing() {
        WorkflowNodeFinishedEvent.PluginBinding binding = new WorkflowNodeFinishedEvent.PluginBinding();
        binding.setPluginId("test-plugin");

        WorkflowNodeFinishedEvent.NodeExtPayload parsedExt = new WorkflowNodeFinishedEvent.NodeExtPayload();
        parsedExt.setPlugins(List.of(binding));

        WorkflowNodeFinishedEvent event = new WorkflowNodeFinishedEvent();
        event.setInstanceId(100L);
        event.setStatus("waiting");
        event.setNodeCode("node1");
        event.setParsedExt(parsedExt);

        DocProcessConfig config = new DocProcessConfig();
        config.setId(1L);
        config.setProjectId(10L);

        when(processConfigService.queryByInstanceId(100L)).thenReturn(config);
        when(projectService.queryById(10L)).thenReturn(null);

        service.handleNodeFinished(event);

        verify(processConfigService).queryByInstanceId(100L);
        verify(projectService).queryById(10L);
        verify(contextService, never()).getOrCreate(anyLong(), anyString(), anyLong());
        verify(pluginExecutor, never()).execute(any());
    }

    // ==================== executePlugin Branch Tests ====================

    @Test
    void shouldRecordGeneratedFilesWhenPluginExecutionSucceeds() {
        // Setup plugin binding
        WorkflowNodeFinishedEvent.PluginBinding binding = new WorkflowNodeFinishedEvent.PluginBinding();
        binding.setPluginId("test-plugin");
        binding.setConfig(Map.of("key", "value"));

        WorkflowNodeFinishedEvent.NodeExtPayload parsedExt = new WorkflowNodeFinishedEvent.NodeExtPayload();
        parsedExt.setPlugins(List.of(binding));

        WorkflowNodeFinishedEvent event = new WorkflowNodeFinishedEvent();
        event.setInstanceId(100L);
        event.setStatus("waiting");
        event.setNodeCode("node1");
        event.setParsedExt(parsedExt);

        DocProcessConfig config = new DocProcessConfig();
        config.setId(1L);
        config.setProjectId(10L);

        DocProjectVo project = new DocProjectVo();
        project.setId(10L);
        project.setName("Test Project");

        DocNodeContext nodeContext = DocNodeContext.create(100L, "node1", 10L);
        nodeContext.setId(1000L);

        DocumentPlugin mockPlugin = mock(DocumentPlugin.class);
        when(mockPlugin.getPluginId()).thenReturn("test-plugin");

        PluginResult.GeneratedFile generatedFile = PluginResult.GeneratedFile.builder()
            .fileName("test.pdf")
            .nasPath("/test/path")
            .build();

        PluginResult successResult = PluginResult.ok(List.of(generatedFile));

        PluginExecutionResult executionResult = PluginExecutionResult.builder()
            .result(successResult)
            .costMs(100L)
            .build();

        when(processConfigService.queryByInstanceId(100L)).thenReturn(config);
        when(projectService.queryById(10L)).thenReturn(project);
        when(contextService.getOrCreate(100L, "node1", 10L)).thenReturn(nodeContext);
        when(contextService.buildReader(100L)).thenReturn(contextReader);
        when(pluginRegistry.getPlugin("test-plugin")).thenReturn(mockPlugin);
        when(pluginExecutor.execute(any())).thenReturn(executionResult);

        // Execute
        service.handleNodeFinished(event);

        ArgumentCaptor<PluginResult.GeneratedFile> fileCaptor = ArgumentCaptor.forClass(PluginResult.GeneratedFile.class);
        verify(documentRecordService, times(1)).recordPluginGenerated(eq(10L), eq("test-plugin"), eq(1000L), fileCaptor.capture());
        assertEquals("test.pdf", fileCaptor.getValue().getFileName());
        assertEquals("/test/path", fileCaptor.getValue().getNasPath());
    }

    /**
     * 成功但无生成文件列表时不应写入文档记录。
     */
    @Test
    void shouldNotRecordWhenGeneratedFilesNullOnSuccess() {
        WorkflowNodeFinishedEvent.PluginBinding binding = new WorkflowNodeFinishedEvent.PluginBinding();
        binding.setPluginId("test-plugin");
        binding.setConfig(Map.of("key", "value"));

        WorkflowNodeFinishedEvent.NodeExtPayload parsedExt = new WorkflowNodeFinishedEvent.NodeExtPayload();
        parsedExt.setPlugins(List.of(binding));

        WorkflowNodeFinishedEvent event = new WorkflowNodeFinishedEvent();
        event.setInstanceId(100L);
        event.setStatus("waiting");
        event.setNodeCode("node1");
        event.setParsedExt(parsedExt);

        DocProcessConfig config = new DocProcessConfig();
        config.setId(1L);
        config.setProjectId(10L);

        DocProjectVo project = new DocProjectVo();
        project.setId(10L);
        project.setName("Test Project");

        DocNodeContext nodeContext = DocNodeContext.create(100L, "node1", 10L);
        nodeContext.setId(1000L);

        DocumentPlugin mockPlugin = mock(DocumentPlugin.class);
        when(mockPlugin.getPluginId()).thenReturn("test-plugin");

        PluginResult successNoFiles = PluginResult.ok();
        PluginExecutionResult executionResult = PluginExecutionResult.builder()
            .result(successNoFiles)
            .costMs(10L)
            .build();

        when(processConfigService.queryByInstanceId(100L)).thenReturn(config);
        when(projectService.queryById(10L)).thenReturn(project);
        when(contextService.getOrCreate(100L, "node1", 10L)).thenReturn(nodeContext);
        when(contextService.buildReader(100L)).thenReturn(contextReader);
        when(pluginRegistry.getPlugin("test-plugin")).thenReturn(mockPlugin);
        when(pluginExecutor.execute(any())).thenReturn(executionResult);

        service.handleNodeFinished(event);

        verify(documentRecordService, never()).recordPluginGenerated(anyLong(), anyString(), anyLong(), any());
    }

    /**
     * 成功且生成文件列表为空时不应写入文档记录。
     */
    @Test
    void shouldNotRecordWhenGeneratedFilesEmptyOnSuccess() {
        WorkflowNodeFinishedEvent.PluginBinding binding = new WorkflowNodeFinishedEvent.PluginBinding();
        binding.setPluginId("test-plugin");
        binding.setConfig(Map.of());

        WorkflowNodeFinishedEvent.NodeExtPayload parsedExt = new WorkflowNodeFinishedEvent.NodeExtPayload();
        parsedExt.setPlugins(List.of(binding));

        WorkflowNodeFinishedEvent event = new WorkflowNodeFinishedEvent();
        event.setInstanceId(100L);
        event.setStatus("waiting");
        event.setNodeCode("node1");
        event.setParsedExt(parsedExt);

        DocProcessConfig config = new DocProcessConfig();
        config.setId(1L);
        config.setProjectId(10L);

        DocProjectVo project = new DocProjectVo();
        project.setId(10L);
        project.setName("Test Project");

        DocNodeContext nodeContext = DocNodeContext.create(100L, "node1", 10L);
        nodeContext.setId(1000L);

        DocumentPlugin mockPlugin = mock(DocumentPlugin.class);
        when(mockPlugin.getPluginId()).thenReturn("test-plugin");

        PluginResult successEmpty = PluginResult.ok(Collections.emptyList());
        PluginExecutionResult executionResult = PluginExecutionResult.builder()
            .result(successEmpty)
            .costMs(5L)
            .build();

        when(processConfigService.queryByInstanceId(100L)).thenReturn(config);
        when(projectService.queryById(10L)).thenReturn(project);
        when(contextService.getOrCreate(100L, "node1", 10L)).thenReturn(nodeContext);
        when(contextService.buildReader(100L)).thenReturn(contextReader);
        when(pluginRegistry.getPlugin("test-plugin")).thenReturn(mockPlugin);
        when(pluginExecutor.execute(any())).thenReturn(executionResult);

        service.handleNodeFinished(event);

        verify(documentRecordService, never()).recordPluginGenerated(anyLong(), anyString(), anyLong(), any());
    }

    @Test
    void shouldNotRecordWhenPluginExecutionFails() {
        // Setup plugin binding
        WorkflowNodeFinishedEvent.PluginBinding binding = new WorkflowNodeFinishedEvent.PluginBinding();
        binding.setPluginId("failing-plugin");
        binding.setConfig(Map.of());

        WorkflowNodeFinishedEvent.NodeExtPayload parsedExt = new WorkflowNodeFinishedEvent.NodeExtPayload();
        parsedExt.setPlugins(List.of(binding));

        WorkflowNodeFinishedEvent event = new WorkflowNodeFinishedEvent();
        event.setInstanceId(100L);
        event.setStatus("waiting");
        event.setNodeCode("node1");
        event.setParsedExt(parsedExt);

        DocProcessConfig config = new DocProcessConfig();
        config.setId(1L);
        config.setProjectId(10L);

        DocProjectVo project = new DocProjectVo();
        project.setId(10L);
        project.setName("Test Project");

        DocNodeContext nodeContext = DocNodeContext.create(100L, "node1", 10L);
        nodeContext.setId(1000L);

        DocumentPlugin mockPlugin = mock(DocumentPlugin.class);
        when(mockPlugin.getPluginId()).thenReturn("failing-plugin");

        PluginResult failedResult = PluginResult.fail("Plugin execution error");
        PluginExecutionResult executionResult = PluginExecutionResult.builder()
            .result(failedResult)
            .costMs(50L)
            .build();

        when(processConfigService.queryByInstanceId(100L)).thenReturn(config);
        when(projectService.queryById(10L)).thenReturn(project);
        when(contextService.getOrCreate(100L, "node1", 10L)).thenReturn(nodeContext);
        when(contextService.buildReader(100L)).thenReturn(contextReader);
        when(pluginRegistry.getPlugin("failing-plugin")).thenReturn(mockPlugin);
        when(pluginExecutor.execute(any())).thenReturn(executionResult);

        // Execute
        service.handleNodeFinished(event);

        // Verify file not recorded on failure
        verify(documentRecordService, never()).recordPluginGenerated(anyLong(), anyString(), anyLong(), any());
    }
}
