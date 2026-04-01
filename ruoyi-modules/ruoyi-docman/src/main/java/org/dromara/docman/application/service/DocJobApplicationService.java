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

    /**
     * 补偿创建此前失败的 NAS 目录。
     *
     * @return 成功补偿数量
     */
    public int retryPendingNasDirectories() {
        return projectService.retryPendingNasDirectories();
    }

    /**
     * 发送待生成文档提醒。
     *
     * @return 发送数量
     */
    public int sendPendingDocumentReminders() {
        return documentReminderService.sendPendingReminders(jobConfig.getDocumentReminderPendingDays());
    }

    /**
     * 发送节点截止日期临近提醒。
     *
     * @return 发送数量
     */
    public int sendNodeDeadlineReminders() {
        return nodeDeadlineService.sendApproachingDeadlineReminders(
            jobConfig.getReminderAdvanceDays(), jobConfig.getMaxReminderCount());
    }
}
