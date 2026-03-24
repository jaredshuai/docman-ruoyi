package org.dromara.docman.application.service;

import lombok.RequiredArgsConstructor;
import org.dromara.common.core.application.CommandApplicationService;
import org.dromara.docman.config.DocmanJobConfig;
import org.dromara.docman.service.IDocDocumentReminderService;
import org.dromara.docman.service.IDocNodeDeadlineService;
import org.dromara.docman.service.IDocProjectService;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DocJobApplicationService implements CommandApplicationService {

    private final IDocProjectService projectService;
    private final IDocDocumentReminderService documentReminderService;
    private final IDocNodeDeadlineService nodeDeadlineService;
    private final DocmanJobConfig jobConfig;

    public int retryPendingNasDirectories() {
        return projectService.retryPendingNasDirectories();
    }

    public int sendPendingDocumentReminders() {
        return documentReminderService.sendPendingReminders(jobConfig.getDocumentReminderPendingDays());
    }

    public int sendNodeDeadlineReminders() {
        return nodeDeadlineService.sendApproachingDeadlineReminders(
            jobConfig.getReminderAdvanceDays(), jobConfig.getMaxReminderCount());
    }
}
