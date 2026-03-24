package org.dromara.docman.infrastructure.workflow;

import org.dromara.common.core.exception.ServiceException;
import org.dromara.warm.flow.core.FlowEngine;
import org.dromara.warm.flow.core.dto.FlowParams;
import org.dromara.warm.flow.core.entity.Definition;
import org.dromara.warm.flow.core.entity.Instance;
import org.dromara.warm.flow.core.service.DefService;
import org.dromara.warm.flow.core.service.InsService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

@Tag("dev")
@Tag("prod")
@Tag("local")
class WarmFlowProcessEngineAdapterTest {

    private static MockedStatic<FlowEngine> flowEngineMock;

    @BeforeAll
    static void setUp() {
        flowEngineMock = mockStatic(FlowEngine.class);
    }

    @AfterAll
    static void tearDown() {
        flowEngineMock.close();
    }

    @Test
    void shouldThrowExceptionWhenDefinitionNotFound() {
        // Arrange
        DefService defService = mock(DefService.class);
        when(defService.getById(anyLong())).thenReturn(null);

        flowEngineMock.when(FlowEngine::defService).thenReturn(defService);

        WarmFlowProcessEngineAdapter adapter = new WarmFlowProcessEngineAdapter();

        // Act & Assert
        ServiceException exception = assertThrows(
            ServiceException.class,
            () -> adapter.startProcess(999L, "business-123", 1L)
        );
        assertEquals("流程定义不存在: 999", exception.getMessage());
    }

    @Test
    void shouldStartProcessSuccessfully() {
        // Arrange
        DefService defService = mock(DefService.class);
        InsService insService = mock(InsService.class);

        Definition definition = mock(Definition.class);
        when(definition.getFlowCode()).thenReturn("doc-approval");

        Instance instance = mock(Instance.class);
        when(instance.getId()).thenReturn(100L);

        when(defService.getById(anyLong())).thenReturn(definition);
        when(insService.start(any(String.class), any(FlowParams.class))).thenReturn(instance);

        flowEngineMock.when(FlowEngine::defService).thenReturn(defService);
        flowEngineMock.when(FlowEngine::insService).thenReturn(insService);

        WarmFlowProcessEngineAdapter adapter = new WarmFlowProcessEngineAdapter();

        // Act
        Long result = adapter.startProcess(1L, "business-456", 2L);

        // Assert
        assertEquals(100L, result);
    }
}