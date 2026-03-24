package org.dromara.docman.service;

import org.dromara.docman.domain.bo.DocNodeDeadlineBo;
import org.dromara.docman.domain.vo.DocNodeDeadlineVo;

import java.util.List;

public interface IDocNodeDeadlineService {

    void createDeadline(Long processInstanceId, String nodeCode, Long projectId, int durationDays);

    List<DocNodeDeadlineVo> listByProject(Long projectId);

    void updateDeadline(DocNodeDeadlineBo bo);

    int sendApproachingDeadlineReminders(int reminderAdvanceDays, int maxReminderCount);
}
