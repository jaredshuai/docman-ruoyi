package org.dromara.docman.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.constant.GlobalConstants;
import org.dromara.common.redis.utils.RedisUtils;
import org.dromara.docman.application.port.out.SystemMessagePort;
import org.dromara.docman.domain.entity.DocDocumentRecord;
import org.dromara.docman.domain.entity.DocProcessConfig;
import org.dromara.docman.domain.entity.DocProject;
import org.dromara.docman.domain.enums.DocProcessConfigStatus;
import org.dromara.docman.domain.enums.DocProjectStatus;
import org.dromara.docman.service.IDocDocumentRecordService;
import org.dromara.docman.service.IDocDocumentReminderService;
import org.dromara.docman.service.IDocProcessConfigService;
import org.dromara.docman.service.IDocProjectService;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocDocumentReminderServiceImpl implements IDocDocumentReminderService {

    private static final String DOCUMENT_REMINDER_KEY_PREFIX = GlobalConstants.GLOBAL_REDIS_KEY + "docman:reminder:date:";

    private final IDocProcessConfigService processConfigService;
    private final IDocProjectService projectService;
    private final IDocDocumentRecordService documentRecordService;
    private final SystemMessagePort systemMessagePort;

    @Override
    public int sendPendingReminders(int overdueDays) {
        int effectiveOverdueDays = Math.max(overdueDays, 1);
        Date cutoffTime = Date.from(Instant.now().minus(effectiveOverdueDays, ChronoUnit.DAYS));

        List<DocProcessConfig> runningConfigs = processConfigService.listByStatus(DocProcessConfigStatus.RUNNING);
        if (runningConfigs.isEmpty()) {
            return 0;
        }

        Set<Long> runningProjectIds = runningConfigs.stream()
            .map(DocProcessConfig::getProjectId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        if (runningProjectIds.isEmpty()) {
            return 0;
        }

        List<DocProject> activeProjects = projectService.listByIdsAndStatus(runningProjectIds, DocProjectStatus.ACTIVE);
        if (activeProjects.isEmpty()) {
            return 0;
        }

        Map<Long, DocProject> projectMap = activeProjects.stream()
            .collect(Collectors.toMap(DocProject::getId, Function.identity()));

        List<DocDocumentRecord> overduePendingRecords = documentRecordService
            .listPendingCreatedBeforeByProjectIds(List.copyOf(projectMap.keySet()), cutoffTime);
        if (overduePendingRecords.isEmpty()) {
            return 0;
        }

        Map<Long, Long> pendingCountByProject = overduePendingRecords.stream()
            .collect(Collectors.groupingBy(DocDocumentRecord::getProjectId, Collectors.counting()));

        int remindedCount = 0;
        LocalDate reminderDate = LocalDate.now();
        for (Map.Entry<Long, Long> entry : pendingCountByProject.entrySet()) {
            DocProject project = projectMap.get(entry.getKey());
            if (project == null || project.getOwnerId() == null) {
                log.warn("跳过文档提醒，项目或负责人缺失: projectId={}", entry.getKey());
                continue;
            }

            String reminderKey = buildReminderKey(reminderDate, project.getId());
            boolean firstReminderToday = RedisUtils.setObjectIfAbsent(reminderKey, "1", Duration.ofHours(25));
            if (!firstReminderToday) {
                log.debug("跳过今日已提醒项目: projectId={}", project.getId());
                continue;
            }

            try {
                systemMessagePort.publishToUsers(
                    List.of(project.getOwnerId()),
                    buildReminderMessage(project, entry.getValue(), effectiveOverdueDays)
                );
            } catch (RuntimeException e) {
                RedisUtils.deleteObject(reminderKey);
                throw e;
            }
            remindedCount++;
        }
        return remindedCount;
    }

    private String buildReminderMessage(DocProject project, Long pendingCount, int overdueDays) {
        return "项目【" + project.getName() + "】存在 " + pendingCount
            + " 份待生成文档已超过 " + overdueDays + " 天，请及时处理。";
    }

    private String buildReminderKey(LocalDate reminderDate, Long projectId) {
        return DOCUMENT_REMINDER_KEY_PREFIX + reminderDate + ":" + projectId;
    }
}
