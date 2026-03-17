package org.dromara.docman.service;

public interface IDocDocumentReminderService {

    int sendPendingReminders(int overdueDays);
}
