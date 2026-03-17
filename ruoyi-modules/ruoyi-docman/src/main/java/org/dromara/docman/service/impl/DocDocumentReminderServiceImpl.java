package org.dromara.docman.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.docman.application.port.out.SystemMessagePort;
import org.dromara.docman.domain.entity.DocDocumentRecord;
import org.dromara.docman.domain.entity.DocProcessConfig;
import org.dromara.docman.domain.entity.DocProject;
import org.dromara.docman.domain.enums.DocDocumentStatus;
import org.dromara.docman.mapper.DocDocumentRecordMapper;
import org.dromara.docman.mapper.DocProcessConfigMapper;
import org.dromara.docman.mapper.DocProjectMapper;
import org.dromara.docman.service.IDocDocumentReminderService;
import org.springframework.stereotype.Service;

import java.time.Instant;
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

    private static final String PROCESS_STATUS_RUNNING = "running";
    private static final String PROJECT_STATUS_ACTIVE = "active";

    private final DocProcessConfigMapper processConfigMapper;
    private final DocProjectMapper projectMapper;
    private final DocDocumentRecordMapper documentRecordMapper;
    private final SystemMessagePort systemMessagePort;

    @Override
    public int sendPendingReminders(int overdueDays) {
        int effectiveOverdueDays = Math.max(overdueDays, 1);
        Date cutoffTime = Date.from(Instant.now().minus(effectiveOverdueDays, ChronoUnit.DAYS));

        List<DocProcessConfig> runningConfigs = processConfigMapper.selectList(
            new LambdaQueryWrapper<DocProcessConfig>()
                .eq(DocProcessConfig::getStatus, PROCESS_STATUS_RUNNING)
                .select(DocProcessConfig::getProjectId)
        );
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

        List<DocProject> activeProjects = projectMapper.selectList(
            new LambdaQueryWrapper<DocProject>()
                .in(DocProject::getId, runningProjectIds)
                .eq(DocProject::getStatus, PROJECT_STATUS_ACTIVE)
        );
        if (activeProjects.isEmpty()) {
            return 0;
        }

        Map<Long, DocProject> projectMap = activeProjects.stream()
            .collect(Collectors.toMap(DocProject::getId, Function.identity()));

        List<DocDocumentRecord> overduePendingRecords = documentRecordMapper.selectList(
            new LambdaQueryWrapper<DocDocumentRecord>()
                .in(DocDocumentRecord::getProjectId, projectMap.keySet())
                .eq(DocDocumentRecord::getStatus, DocDocumentStatus.PENDING.getCode())
                .le(DocDocumentRecord::getCreateTime, cutoffTime)
                .select(DocDocumentRecord::getProjectId)
        );
        if (overduePendingRecords.isEmpty()) {
            return 0;
        }

        Map<Long, Long> pendingCountByProject = overduePendingRecords.stream()
            .collect(Collectors.groupingBy(DocDocumentRecord::getProjectId, Collectors.counting()));

        int remindedCount = 0;
        for (Map.Entry<Long, Long> entry : pendingCountByProject.entrySet()) {
            DocProject project = projectMap.get(entry.getKey());
            if (project == null || project.getOwnerId() == null) {
                log.warn("跳过文档提醒，项目或负责人缺失: projectId={}", entry.getKey());
                continue;
            }
            systemMessagePort.publishToUsers(
                List.of(project.getOwnerId()),
                buildReminderMessage(project, entry.getValue(), effectiveOverdueDays)
            );
            remindedCount++;
        }
        return remindedCount;
    }

    private String buildReminderMessage(DocProject project, Long pendingCount, int overdueDays) {
        return "项目【" + project.getName() + "】存在 " + pendingCount
            + " 份待生成文档已超过 " + overdueDays + " 天，请及时处理。";
    }
}
