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
import static org.mockito.Mockito.never;
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
}