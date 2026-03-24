package org.dromara.docman.application.service;

import org.dromara.docman.config.DocmanJobConfig;
import org.dromara.docman.service.IDocDocumentReminderService;
import org.dromara.docman.service.IDocNodeDeadlineService;
import org.dromara.docman.service.IDocProjectService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("dev")
@Tag("prod")
@Tag("local")
class DocJobApplicationServiceTest {

    @Mock
    private IDocProjectService projectService;

    @Mock
    private IDocDocumentReminderService documentReminderService;

    @Mock
    private IDocNodeDeadlineService nodeDeadlineService;

    @Mock
    private DocmanJobConfig jobConfig;

    @InjectMocks
    private DocJobApplicationService applicationService;

    @Test
    void shouldRetryPendingNasDirectories() {
        when(projectService.retryPendingNasDirectories()).thenReturn(5);

        int result = applicationService.retryPendingNasDirectories();

        assertEquals(5, result);
        verify(projectService).retryPendingNasDirectories();
    }

    @Test
    void shouldSendPendingDocumentRemindersWithConfigValue() {
        when(jobConfig.getDocumentReminderPendingDays()).thenReturn(7);
        when(documentReminderService.sendPendingReminders(7)).thenReturn(10);

        int result = applicationService.sendPendingDocumentReminders();

        assertEquals(10, result);
        verify(jobConfig).getDocumentReminderPendingDays();
        verify(documentReminderService).sendPendingReminders(7);
    }

    @Test
    void shouldSendNodeDeadlineRemindersWithConfigValues() {
        when(jobConfig.getReminderAdvanceDays()).thenReturn(5);
        when(jobConfig.getMaxReminderCount()).thenReturn(3);
        when(nodeDeadlineService.sendApproachingDeadlineReminders(5, 3)).thenReturn(8);

        int result = applicationService.sendNodeDeadlineReminders();

        assertEquals(8, result);
        verify(jobConfig).getReminderAdvanceDays();
        verify(jobConfig).getMaxReminderCount();
        verify(nodeDeadlineService).sendApproachingDeadlineReminders(5, 3);
    }
}