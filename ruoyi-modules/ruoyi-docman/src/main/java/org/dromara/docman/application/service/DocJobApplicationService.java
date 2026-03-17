package org.dromara.docman.application.service;

import lombok.RequiredArgsConstructor;
import org.dromara.common.core.application.CommandApplicationService;
import org.dromara.docman.config.DocmanJobConfig;
import org.dromara.docman.service.IDocDocumentReminderService;
import org.dromara.docman.service.IDocProjectService;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DocJobApplicationService implements CommandApplicationService {

    private final IDocProjectService projectService;
    private final IDocDocumentReminderService documentReminderService;
    private final DocmanJobConfig jobConfig;

    public int retryPendingNasDirectories() {
        return projectService.retryPendingNasDirectories();
    }

    public int sendPendingDocumentReminders() {
        return documentReminderService.sendPendingReminders(jobConfig.getDocumentReminderPendingDays());
    }
}
