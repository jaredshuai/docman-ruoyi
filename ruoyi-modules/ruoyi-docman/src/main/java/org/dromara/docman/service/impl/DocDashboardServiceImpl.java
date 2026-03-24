package org.dromara.docman.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.docman.domain.entity.DocDocumentRecord;
import org.dromara.docman.domain.entity.DocNodeDeadline;
import org.dromara.docman.domain.entity.DocPluginExecutionLog;
import org.dromara.docman.domain.entity.DocProject;
import org.dromara.docman.domain.enums.DocDocumentStatus;
import org.dromara.docman.domain.enums.DocPluginExecutionStatus;
import org.dromara.docman.domain.enums.DocProjectStatus;
import org.dromara.docman.domain.vo.DocDashboardOverviewVo;
import org.dromara.docman.domain.vo.DocDeadlineAlertVo;
import org.dromara.docman.domain.vo.DocPluginStatsVo;
import org.dromara.docman.domain.vo.DocProjectProgressVo;
import org.dromara.docman.mapper.DocDashboardMapper;
import org.dromara.docman.mapper.DocDocumentRecordMapper;
import org.dromara.docman.mapper.DocNodeDeadlineMapper;
import org.dromara.docman.mapper.DocPluginExecutionLogMapper;
import org.dromara.docman.mapper.DocProjectMapper;
import org.dromara.docman.service.IDocDashboardService;
import org.dromara.docman.service.IDocProjectAccessService;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DocDashboardServiceImpl implements IDocDashboardService {

    private final DocDashboardMapper dashboardMapper;
    private final DocProjectMapper projectMapper;
    private final DocDocumentRecordMapper documentRecordMapper;
    private final DocNodeDeadlineMapper nodeDeadlineMapper;
    private final DocPluginExecutionLogMapper pluginExecutionLogMapper;
    private final IDocProjectAccessService projectAccessService;

    @Override
    public DocDashboardOverviewVo getOverview() {
        DocDashboardOverviewVo overview = new DocDashboardOverviewVo();
        List<Long> projectIds = listAccessibleProjectIds();
        if (projectIds.isEmpty()) {
            overview.setTotalProjects(0L);
            overview.setActiveProjects(0L);
            overview.setTotalDocuments(0L);
            overview.setPendingDocuments(0L);
            overview.setOverdueNodes(0L);
            overview.setPluginFailCount(0L);
            return overview;
        }

        overview.setTotalProjects(countProjects(projectIds, null));
        overview.setActiveProjects(countProjects(projectIds, DocProjectStatus.ACTIVE.getCode()));
        overview.setTotalDocuments(countDocuments(projectIds, null));
        overview.setPendingDocuments(countDocuments(projectIds, DocDocumentStatus.PENDING.getCode()));
        overview.setOverdueNodes(countOverdueNodes(projectIds, LocalDate.now()));
        overview.setPluginFailCount(countPluginFailures(projectIds, Timestamp.valueOf(LocalDateTime.now().minusDays(7))));
        return overview;
    }

    @Override
    public List<DocProjectProgressVo> listProjectProgress() {
        List<Long> projectIds = listAccessibleProjectIds();
        if (projectIds.isEmpty()) {
            return List.of();
        }
        return dashboardMapper.selectProjectProgress(projectIds);
    }

    @Override
    public List<DocDeadlineAlertVo> listDeadlineAlerts() {
        List<Long> projectIds = listAccessibleProjectIds();
        if (projectIds.isEmpty()) {
            return List.of();
        }
        LocalDate today = LocalDate.now();
        return dashboardMapper.selectDeadlineAlerts(projectIds, today, today.plusDays(7));
    }

    @Override
    public List<DocPluginStatsVo> listPluginStats() {
        List<Long> projectIds = listAccessibleProjectIds();
        if (projectIds.isEmpty()) {
            return List.of();
        }
        return dashboardMapper.selectPluginStats(projectIds, Timestamp.valueOf(LocalDateTime.now().minusDays(30)));
    }

    private List<Long> listAccessibleProjectIds() {
        List<Long> projectIds = projectAccessService.listAccessibleProjectIds(LoginHelper.getUserId());
        if (projectIds.isEmpty()) {
            return List.of();
        }
        return projectMapper.selectList(
            new LambdaQueryWrapper<DocProject>()
                .in(DocProject::getId, projectIds)
                .select(DocProject::getId)
        ).stream().map(DocProject::getId).toList();
    }

    private Long countProjects(List<Long> projectIds, String status) {
        return projectMapper.selectCount(
            new LambdaQueryWrapper<DocProject>()
                .in(DocProject::getId, projectIds)
                .eq(status != null, DocProject::getStatus, status)
        );
    }

    private Long countDocuments(List<Long> projectIds, String status) {
        return documentRecordMapper.selectCount(
            new LambdaQueryWrapper<DocDocumentRecord>()
                .in(DocDocumentRecord::getProjectId, projectIds)
                .eq(status != null, DocDocumentRecord::getStatus, status)
        );
    }

    private Long countOverdueNodes(List<Long> projectIds, LocalDate today) {
        return nodeDeadlineMapper.selectCount(
            new LambdaQueryWrapper<DocNodeDeadline>()
                .in(DocNodeDeadline::getProjectId, projectIds)
                .lt(DocNodeDeadline::getDeadline, today)
        );
    }

    private Long countPluginFailures(List<Long> projectIds, Timestamp createdAfter) {
        return pluginExecutionLogMapper.selectCount(
            new LambdaQueryWrapper<DocPluginExecutionLog>()
                .in(DocPluginExecutionLog::getProjectId, projectIds)
                .eq(DocPluginExecutionLog::getStatus, DocPluginExecutionStatus.FAILED.getCode())
                .ge(DocPluginExecutionLog::getCreateTime, createdAfter)
        );
    }
}
