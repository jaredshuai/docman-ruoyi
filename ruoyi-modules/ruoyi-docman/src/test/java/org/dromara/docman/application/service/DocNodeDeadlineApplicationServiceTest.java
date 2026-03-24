package org.dromara.docman.application.service;

import org.dromara.docman.domain.bo.DocNodeDeadlineBo;
import org.dromara.docman.service.IDocNodeDeadlineService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@Tag("dev")
@Tag("prod")
@Tag("local")
class DocNodeDeadlineApplicationServiceTest {

    @Mock
    private IDocNodeDeadlineService nodeDeadlineService;

    @InjectMocks
    private DocNodeDeadlineApplicationService applicationService;

    @Test
    void update_shouldDelegateToNodeDeadlineService() {
        // Arrange
        DocNodeDeadlineBo bo = new DocNodeDeadlineBo();
        bo.setId(1L);
        bo.setDeadline(LocalDate.of(2026, 3, 30));
        bo.setDurationDays(7);

        // Act
        applicationService.update(bo);

        // Assert
        ArgumentCaptor<DocNodeDeadlineBo> captor = ArgumentCaptor.forClass(DocNodeDeadlineBo.class);
        verify(nodeDeadlineService).updateDeadline(captor.capture());
        assertEquals(1L, captor.getValue().getId());
        assertEquals(LocalDate.of(2026, 3, 30), captor.getValue().getDeadline());
        assertEquals(7, captor.getValue().getDurationDays());
    }

    @Test
    void update_shouldPassBoWithNullDeadline() {
        // Arrange
        DocNodeDeadlineBo bo = new DocNodeDeadlineBo();
        bo.setId(2L);
        bo.setDeadline(null);
        bo.setDurationDays(5);

        // Act
        applicationService.update(bo);

        // Assert
        ArgumentCaptor<DocNodeDeadlineBo> captor = ArgumentCaptor.forClass(DocNodeDeadlineBo.class);
        verify(nodeDeadlineService).updateDeadline(captor.capture());
        assertEquals(2L, captor.getValue().getId());
        assertNull(captor.getValue().getDeadline());
        assertEquals(5, captor.getValue().getDurationDays());
    }

    @Test
    void update_shouldPassBoWithNullDurationDays() {
        // Arrange
        DocNodeDeadlineBo bo = new DocNodeDeadlineBo();
        bo.setId(3L);
        bo.setDeadline(LocalDate.of(2026, 4, 15));
        bo.setDurationDays(null);

        // Act
        applicationService.update(bo);

        // Assert
        ArgumentCaptor<DocNodeDeadlineBo> captor = ArgumentCaptor.forClass(DocNodeDeadlineBo.class);
        verify(nodeDeadlineService).updateDeadline(captor.capture());
        assertEquals(3L, captor.getValue().getId());
        assertEquals(LocalDate.of(2026, 4, 15), captor.getValue().getDeadline());
        assertNull(captor.getValue().getDurationDays());
    }

    @Test
    void update_shouldCallServiceExactlyOnce() {
        // Arrange
        DocNodeDeadlineBo bo = new DocNodeDeadlineBo();
        bo.setId(4L);

        // Act
        applicationService.update(bo);

        // Assert
        verify(nodeDeadlineService, times(1)).updateDeadline(any());
    }
}