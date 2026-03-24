package org.dromara.docman.application.service;

import org.dromara.docman.service.IDocProcessService;
import org.dromara.warm.flow.core.entity.Definition;
import org.dromara.warm.flow.core.enums.PublishStatus;
import org.dromara.warm.flow.core.service.DefService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@Tag("dev")
@Tag("prod")
@Tag("local")
class DocProcessApplicationServiceTest {

    @Mock
    private IDocProcessService processService;

    @Mock
    private DefService definitionService;

    @InjectMocks
    private DocProcessApplicationService applicationService;

    @Test
    void bind_shouldDelegateToProcessService() {
        // Arrange
        Long projectId = 1L;
        Long definitionId = 100L;

        // Act
        applicationService.bind(projectId, definitionId);

        // Assert
        verify(processService).bindProcess(projectId, definitionId);
    }

    @Test
    void start_shouldDelegateToProcessServiceAndReturnResult() {
        // Arrange
        Long projectId = 1L;
        Long expectedInstanceId = 500L;
        when(processService.startProcess(projectId)).thenReturn(expectedInstanceId);

        // Act
        Long result = applicationService.start(projectId);

        // Assert
        assertEquals(expectedInstanceId, result);
        verify(processService).startProcess(projectId);
    }

    @Test
    void listDefinitions_shouldQueryPublishedDefinitionsAndMapResult() {
        // Arrange
        Definition def1 = mock(Definition.class);
        when(def1.getId()).thenReturn(1L);
        when(def1.getFlowName()).thenReturn("Process A");

        Definition def2 = mock(Definition.class);
        when(def2.getId()).thenReturn(2L);
        when(def2.getFlowName()).thenReturn("Process B");

        List<Definition> mockDefinitions = Arrays.asList(def1, def2);

        ArgumentCaptor<org.dromara.warm.flow.orm.entity.FlowDefinition> queryCaptor =
            ArgumentCaptor.forClass(org.dromara.warm.flow.orm.entity.FlowDefinition.class);

        when(definitionService.list(any())).thenReturn(mockDefinitions);

        // Act
        List<Map<String, Object>> result = applicationService.listDefinitions();

        // Assert
        verify(definitionService).list(queryCaptor.capture());
        assertEquals(PublishStatus.PUBLISHED.getKey(), queryCaptor.getValue().getIsPublish());

        assertEquals(2, result.size());

        Map<String, Object> first = result.get(0);
        assertEquals(1L, first.get("id"));
        assertEquals("Process A", first.get("name"));

        Map<String, Object> second = result.get(1);
        assertEquals(2L, second.get("id"));
        assertEquals("Process B", second.get("name"));
    }

    @Test
    void listDefinitions_shouldReturnEmptyListWhenNoDefinitions() {
        // Arrange
        when(definitionService.list(any())).thenReturn(Collections.emptyList());

        // Act
        List<Map<String, Object>> result = applicationService.listDefinitions();

        // Assert
        assertTrue(result.isEmpty());
    }
}