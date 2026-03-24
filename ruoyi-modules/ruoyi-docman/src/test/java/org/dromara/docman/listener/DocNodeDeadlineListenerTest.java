package org.dromara.docman.listener;

import org.dromara.common.core.domain.event.ProcessTaskEvent;
import org.dromara.docman.domain.entity.DocProcessConfig;
import org.dromara.docman.service.IDocNodeDeadlineService;
import org.dromara.docman.service.IDocProcessConfigService;
import org.dromara.warm.flow.core.entity.Node;
import org.dromara.warm.flow.core.service.NodeService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("dev")
@Tag("prod")
@Tag("local")
class DocNodeDeadlineListenerTest {

    @Mock
    private IDocNodeDeadlineService nodeDeadlineService;

    @Mock
    private IDocProcessConfigService processConfigService;

    @Mock
    private NodeService nodeService;

    @InjectMocks
    private DocNodeDeadlineListener listener;

    @Test
    void shouldReturnEarlyWhenInstanceIdIsNull() {
        ProcessTaskEvent event = new ProcessTaskEvent();
        event.setInstanceId(null);
        event.setNodeCode("node-1");

        listener.onTaskCreated(event);

        verify(processConfigService, never()).queryByInstanceId(anyLong());
        verify(nodeDeadlineService, never()).createDeadline(anyLong(), anyString(), anyLong(), any(int.class));
    }

    @Test
    void shouldReturnEarlyWhenNodeCodeIsBlank() {
        ProcessTaskEvent event = new ProcessTaskEvent();
        event.setInstanceId(1L);
        event.setNodeCode("");

        listener.onTaskCreated(event);

        verify(processConfigService, never()).queryByInstanceId(anyLong());
        verify(nodeDeadlineService, never()).createDeadline(anyLong(), anyString(), anyLong(), any(int.class));
    }

    @Test
    void shouldReturnEarlyWhenNodeCodeIsNull() {
        ProcessTaskEvent event = new ProcessTaskEvent();
        event.setInstanceId(1L);
        event.setNodeCode(null);

        listener.onTaskCreated(event);

        verify(processConfigService, never()).queryByInstanceId(anyLong());
        verify(nodeDeadlineService, never()).createDeadline(anyLong(), anyString(), anyLong(), any(int.class));
    }

    @Test
    void shouldReturnEarlyWhenConfigIsNull() {
        ProcessTaskEvent event = new ProcessTaskEvent();
        event.setInstanceId(1L);
        event.setNodeCode("node-1");
        when(processConfigService.queryByInstanceId(1L)).thenReturn(null);

        listener.onTaskCreated(event);

        verify(nodeService, never()).getByDefIdAndNodeCode(anyLong(), anyString());
        verify(nodeDeadlineService, never()).createDeadline(anyLong(), anyString(), anyLong(), any(int.class));
    }

    @Test
    void shouldReturnEarlyWhenNodeIsNull() {
        ProcessTaskEvent event = new ProcessTaskEvent();
        event.setInstanceId(1L);
        event.setNodeCode("node-1");

        DocProcessConfig config = new DocProcessConfig();
        config.setDefinitionId(100L);
        config.setProjectId(200L);
        when(processConfigService.queryByInstanceId(1L)).thenReturn(config);
        when(nodeService.getByDefIdAndNodeCode(100L, "node-1")).thenReturn(null);

        listener.onTaskCreated(event);

        verify(nodeDeadlineService, never()).createDeadline(anyLong(), anyString(), anyLong(), any(int.class));
    }

    @Test
    void shouldReturnEarlyWhenNodeExtIsBlank() {
        ProcessTaskEvent event = new ProcessTaskEvent();
        event.setInstanceId(1L);
        event.setNodeCode("node-1");

        DocProcessConfig config = new DocProcessConfig();
        config.setDefinitionId(100L);
        config.setProjectId(200L);
        when(processConfigService.queryByInstanceId(1L)).thenReturn(config);

        Node node = mock(Node.class);
        when(node.getExt()).thenReturn("");
        when(nodeService.getByDefIdAndNodeCode(100L, "node-1")).thenReturn(node);

        listener.onTaskCreated(event);

        verify(nodeDeadlineService, never()).createDeadline(anyLong(), anyString(), anyLong(), any(int.class));
    }

    @Test
    void shouldReturnEarlyWhenNodeExtIsNull() {
        ProcessTaskEvent event = new ProcessTaskEvent();
        event.setInstanceId(1L);
        event.setNodeCode("node-1");

        DocProcessConfig config = new DocProcessConfig();
        config.setDefinitionId(100L);
        config.setProjectId(200L);
        when(processConfigService.queryByInstanceId(1L)).thenReturn(config);

        Node node = mock(Node.class);
        when(node.getExt()).thenReturn(null);
        when(nodeService.getByDefIdAndNodeCode(100L, "node-1")).thenReturn(node);

        listener.onTaskCreated(event);

        verify(nodeDeadlineService, never()).createDeadline(anyLong(), anyString(), anyLong(), any(int.class));
    }

    @Test
    void shouldReturnEarlyWhenDurationDaysIsZero() {
        ProcessTaskEvent event = new ProcessTaskEvent();
        event.setInstanceId(1L);
        event.setNodeCode("node-1");

        DocProcessConfig config = new DocProcessConfig();
        config.setDefinitionId(100L);
        config.setProjectId(200L);
        when(processConfigService.queryByInstanceId(1L)).thenReturn(config);

        Node node = mock(Node.class);
        when(node.getExt()).thenReturn("{\"durationDays\":0}");
        when(nodeService.getByDefIdAndNodeCode(100L, "node-1")).thenReturn(node);

        listener.onTaskCreated(event);

        verify(nodeDeadlineService, never()).createDeadline(anyLong(), anyString(), anyLong(), any(int.class));
    }

    @Test
    void shouldReturnEarlyWhenDurationDaysIsNegative() {
        ProcessTaskEvent event = new ProcessTaskEvent();
        event.setInstanceId(1L);
        event.setNodeCode("node-1");

        DocProcessConfig config = new DocProcessConfig();
        config.setDefinitionId(100L);
        config.setProjectId(200L);
        when(processConfigService.queryByInstanceId(1L)).thenReturn(config);

        Node node = mock(Node.class);
        when(node.getExt()).thenReturn("{\"durationDays\":-5}");
        when(nodeService.getByDefIdAndNodeCode(100L, "node-1")).thenReturn(node);

        listener.onTaskCreated(event);

        verify(nodeDeadlineService, never()).createDeadline(anyLong(), anyString(), anyLong(), any(int.class));
    }

    @Test
    void shouldReturnEarlyWhenDurationDaysNotInExt() {
        ProcessTaskEvent event = new ProcessTaskEvent();
        event.setInstanceId(1L);
        event.setNodeCode("node-1");

        DocProcessConfig config = new DocProcessConfig();
        config.setDefinitionId(100L);
        config.setProjectId(200L);
        when(processConfigService.queryByInstanceId(1L)).thenReturn(config);

        Node node = mock(Node.class);
        when(node.getExt()).thenReturn("{\"otherField\":\"value\"}");
        when(nodeService.getByDefIdAndNodeCode(100L, "node-1")).thenReturn(node);

        listener.onTaskCreated(event);

        verify(nodeDeadlineService, never()).createDeadline(anyLong(), anyString(), anyLong(), any(int.class));
    }

    @Test
    void shouldReturnEarlyWhenExtIsInvalidJson() {
        ProcessTaskEvent event = new ProcessTaskEvent();
        event.setInstanceId(1L);
        event.setNodeCode("node-1");

        DocProcessConfig config = new DocProcessConfig();
        config.setDefinitionId(100L);
        config.setProjectId(200L);
        when(processConfigService.queryByInstanceId(1L)).thenReturn(config);

        Node node = mock(Node.class);
        when(node.getExt()).thenReturn("invalid-json");
        when(nodeService.getByDefIdAndNodeCode(100L, "node-1")).thenReturn(node);

        listener.onTaskCreated(event);

        verify(nodeDeadlineService, never()).createDeadline(anyLong(), anyString(), anyLong(), any(int.class));
    }

    @Test
    void shouldCreateDeadlineWhenDurationDaysIsPositive() {
        ProcessTaskEvent event = new ProcessTaskEvent();
        event.setInstanceId(1L);
        event.setNodeCode("node-1");

        DocProcessConfig config = new DocProcessConfig();
        config.setDefinitionId(100L);
        config.setProjectId(200L);
        when(processConfigService.queryByInstanceId(1L)).thenReturn(config);

        Node node = mock(Node.class);
        when(node.getExt()).thenReturn("{\"durationDays\":5}");
        when(nodeService.getByDefIdAndNodeCode(100L, "node-1")).thenReturn(node);

        listener.onTaskCreated(event);

        verify(nodeDeadlineService).createDeadline(1L, "node-1", 200L, 5);
    }

    @Test
    void shouldCreateDeadlineWhenDurationDaysInExtraObject() {
        ProcessTaskEvent event = new ProcessTaskEvent();
        event.setInstanceId(1L);
        event.setNodeCode("node-1");

        DocProcessConfig config = new DocProcessConfig();
        config.setDefinitionId(100L);
        config.setProjectId(200L);
        when(processConfigService.queryByInstanceId(1L)).thenReturn(config);

        Node node = mock(Node.class);
        when(node.getExt()).thenReturn("{\"extra\":{\"durationDays\":7}}");
        when(nodeService.getByDefIdAndNodeCode(100L, "node-1")).thenReturn(node);

        listener.onTaskCreated(event);

        verify(nodeDeadlineService).createDeadline(1L, "node-1", 200L, 7);
    }

    @Test
    void shouldHandleExceptionWithoutThrowing() {
        ProcessTaskEvent event = new ProcessTaskEvent();
        event.setInstanceId(1L);
        event.setNodeCode("node-1");

        when(processConfigService.queryByInstanceId(1L)).thenThrow(new RuntimeException("DB error"));

        listener.onTaskCreated(event);

        verify(nodeDeadlineService, never()).createDeadline(anyLong(), anyString(), anyLong(), any(int.class));
    }
}