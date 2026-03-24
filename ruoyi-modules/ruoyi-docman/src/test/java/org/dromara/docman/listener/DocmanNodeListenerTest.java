package org.dromara.docman.listener;

import org.dromara.common.core.domain.event.WorkflowNodeFinishedEvent;
import org.dromara.docman.application.service.DocWorkflowNodeApplicationService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * DocmanNodeListener 单元测试
 * 直接测试委托和异常吞没行为，不依赖 Spring 上下文
 */
@ExtendWith(MockitoExtension.class)
@Tag("dev")
@Tag("prod")
@Tag("local")
class DocmanNodeListenerTest {

    @Mock
    private DocWorkflowNodeApplicationService workflowNodeApplicationService;

    @Test
    void shouldDelegateToServiceOnNodeFinish() {
        // Given
        DocmanNodeListener listener = new DocmanNodeListener(workflowNodeApplicationService);
        WorkflowNodeFinishedEvent event = new WorkflowNodeFinishedEvent();
        event.setFlowCode("doc-approval");
        event.setInstanceId(1L);
        event.setBusinessId("DOC-001");

        // When
        listener.onNodeFinish(event);

        // Then
        verify(workflowNodeApplicationService).handleNodeFinished(event);
        verifyNoMoreInteractions(workflowNodeApplicationService);
    }

    @Test
    void shouldSwallowExceptionWhenServiceThrows() {
        // Given
        DocmanNodeListener listener = new DocmanNodeListener(workflowNodeApplicationService);
        WorkflowNodeFinishedEvent event = new WorkflowNodeFinishedEvent();
        event.setFlowCode("doc-approval");
        event.setInstanceId(2L);

        doThrow(new RuntimeException("Service failure")).when(workflowNodeApplicationService).handleNodeFinished(event);

        // When - should not throw
        listener.onNodeFinish(event);

        // Then - exception is swallowed, service was still called
        verify(workflowNodeApplicationService).handleNodeFinished(event);
    }
}