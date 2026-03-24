package org.dromara.docman.application.service;

import org.dromara.docman.domain.vo.DocNodeDeadlineVo;
import org.dromara.docman.service.IDocNodeDeadlineService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@Tag("dev")
@Tag("prod")
@Tag("local")
class DocNodeDeadlineQueryApplicationServiceTest {

    @Mock
    private IDocNodeDeadlineService nodeDeadlineService;

    @InjectMocks
    private DocNodeDeadlineQueryApplicationService applicationService;

    @Test
    void listByProject_shouldReturnDeadlinesFromService() {
        // Arrange
        Long projectId = 1L;

        DocNodeDeadlineVo vo1 = new DocNodeDeadlineVo();
        vo1.setId(1L);
        vo1.setProjectId(projectId);
        vo1.setNodeCode("node1");
        vo1.setDeadline(LocalDate.of(2026, 3, 30));

        DocNodeDeadlineVo vo2 = new DocNodeDeadlineVo();
        vo2.setId(2L);
        vo2.setProjectId(projectId);
        vo2.setNodeCode("node2");
        vo2.setDeadline(LocalDate.of(2026, 4, 15));

        List<DocNodeDeadlineVo> expectedList = Arrays.asList(vo1, vo2);
        when(nodeDeadlineService.listByProject(projectId)).thenReturn(expectedList);

        // Act
        List<DocNodeDeadlineVo> result = applicationService.listByProject(projectId);

        // Assert
        assertEquals(2, result.size());
        assertEquals(1L, result.get(0).getId());
        assertEquals(2L, result.get(1).getId());
        verify(nodeDeadlineService).listByProject(projectId);
    }

    @Test
    void listByProject_shouldReturnEmptyListWhenNoDeadlines() {
        // Arrange
        Long projectId = 999L;
        when(nodeDeadlineService.listByProject(projectId)).thenReturn(Collections.emptyList());

        // Act
        List<DocNodeDeadlineVo> result = applicationService.listByProject(projectId);

        // Assert
        assertTrue(result.isEmpty());
        verify(nodeDeadlineService).listByProject(projectId);
    }

    @Test
    void listByProject_shouldPassCorrectProjectId() {
        // Arrange
        Long projectId = 42L;
        when(nodeDeadlineService.listByProject(anyLong())).thenReturn(Collections.emptyList());

        // Act
        applicationService.listByProject(projectId);

        // Assert
        verify(nodeDeadlineService).listByProject(42L);
    }

    @Test
    void listByProject_shouldPassthroughAllVoFields() {
        // Arrange
        Long projectId = 1L;

        DocNodeDeadlineVo vo = new DocNodeDeadlineVo();
        vo.setId(10L);
        vo.setProcessInstanceId(100L);
        vo.setNodeCode("approval");
        vo.setProjectId(projectId);
        vo.setDurationDays(7);
        vo.setDeadline(LocalDate.of(2026, 3, 25));
        vo.setReminderCount(2);
        vo.setLastRemindedAt(LocalDateTime.of(2026, 3, 20, 10, 30));
        vo.setProjectName("Test Project");
        vo.setNodeName("Approval Node");
        vo.setCreateTime(LocalDateTime.of(2026, 3, 1, 9, 0));
        vo.setUpdateTime(LocalDateTime.of(2026, 3, 15, 14, 30));

        when(nodeDeadlineService.listByProject(projectId)).thenReturn(List.of(vo));

        // Act
        List<DocNodeDeadlineVo> result = applicationService.listByProject(projectId);

        // Assert
        assertEquals(1, result.size());
        DocNodeDeadlineVo resultVo = result.get(0);
        assertEquals(10L, resultVo.getId());
        assertEquals(100L, resultVo.getProcessInstanceId());
        assertEquals("approval", resultVo.getNodeCode());
        assertEquals(projectId, resultVo.getProjectId());
        assertEquals(7, resultVo.getDurationDays());
        assertEquals(LocalDate.of(2026, 3, 25), resultVo.getDeadline());
        assertEquals(2, resultVo.getReminderCount());
        assertEquals(LocalDateTime.of(2026, 3, 20, 10, 30), resultVo.getLastRemindedAt());
        assertEquals("Test Project", resultVo.getProjectName());
        assertEquals("Approval Node", resultVo.getNodeName());
        assertEquals(LocalDateTime.of(2026, 3, 1, 9, 0), resultVo.getCreateTime());
        assertEquals(LocalDateTime.of(2026, 3, 15, 14, 30), resultVo.getUpdateTime());
    }
}