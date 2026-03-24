package org.dromara.docman.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.dromara.docman.domain.entity.DocProject;
import org.dromara.docman.domain.vo.DocDashboardOverviewVo;
import org.dromara.docman.domain.vo.DocDeadlineAlertVo;
import org.dromara.docman.domain.vo.DocPluginStatsVo;
import org.dromara.docman.domain.vo.DocProjectProgressVo;
import org.dromara.docman.domain.vo.DocTodoSummaryVo;
import org.dromara.docman.mapper.DocDashboardMapper;
import org.dromara.docman.mapper.DocDocumentRecordMapper;
import org.dromara.docman.mapper.DocNodeDeadlineMapper;
import org.dromara.docman.mapper.DocPluginExecutionLogMapper;
import org.dromara.docman.mapper.DocProjectMapper;
import org.dromara.docman.service.IDocProjectAccessService;
import org.dromara.workflow.service.IFlwTaskService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("dev")
@Tag("prod")
@Tag("local")
class DocDashboardServiceImplTest {

    @BeforeAll
    static void initTableInfo() {
        if (TableInfoHelper.getTableInfo(DocProject.class) == null) {
            TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), "test"), DocProject.class);
        }
    }

    @Mock
    private DocDashboardMapper dashboardMapper;

    @Mock
    private DocProjectMapper projectMapper;

    @Mock
    private DocDocumentRecordMapper documentRecordMapper;

    @Mock
    private DocNodeDeadlineMapper nodeDeadlineMapper;

    @Mock
    private DocPluginExecutionLogMapper pluginExecutionLogMapper;

    @Mock
    private IDocProjectAccessService projectAccessService;

    @Mock
    private IFlwTaskService flwTaskService;

    @InjectMocks
    private DocDashboardServiceImpl service;

    @Test
    void shouldReturnZeroOverviewWhenNoAccessibleProjects() {
        when(projectAccessService.listAccessibleProjectIds(null)).thenReturn(List.of());

        DocDashboardOverviewVo overview = service.getOverview();

        assertEquals(0L, overview.getTotalProjects());
        assertEquals(0L, overview.getActiveProjects());
        assertEquals(0L, overview.getTotalDocuments());
        assertEquals(0L, overview.getPendingDocuments());
        assertEquals(0L, overview.getOverdueNodes());
        assertEquals(0L, overview.getPluginFailCount());
        verify(projectMapper, never()).selectList(any());
    }

    @Test
    void shouldAggregateOverviewForAccessibleProjects() {
        when(projectAccessService.listAccessibleProjectIds(null)).thenReturn(List.of(1L, 2L));

        DocProject project = new DocProject();
        project.setId(1L);
        when(projectMapper.selectList(any())).thenReturn(List.of(project));
        when(projectMapper.selectCount(any())).thenReturn(1L, 1L);
        when(documentRecordMapper.selectCount(any())).thenReturn(2L, 1L);
        when(nodeDeadlineMapper.selectCount(any())).thenReturn(1L);
        when(pluginExecutionLogMapper.selectCount(any())).thenReturn(3L);

        DocDashboardOverviewVo overview = service.getOverview();

        assertEquals(1L, overview.getTotalProjects());
        assertEquals(1L, overview.getActiveProjects());
        assertEquals(2L, overview.getTotalDocuments());
        assertEquals(1L, overview.getPendingDocuments());
        assertEquals(1L, overview.getOverdueNodes());
        assertEquals(3L, overview.getPluginFailCount());
    }

    @Test
    void shouldDelegateProjectProgressWithResolvedProjectIds() {
        when(projectAccessService.listAccessibleProjectIds(null)).thenReturn(List.of(1L));
        DocProject project = new DocProject();
        project.setId(1L);
        when(projectMapper.selectList(any())).thenReturn(List.of(project));

        DocProjectProgressVo progressVo = new DocProjectProgressVo();
        progressVo.setProjectId(1L);
        progressVo.setProjectName("演示项目");
        when(dashboardMapper.selectProjectProgress(List.of(1L))).thenReturn(List.of(progressVo));

        List<DocProjectProgressVo> result = service.listProjectProgress();

        assertEquals(1, result.size());
        assertEquals("演示项目", result.get(0).getProjectName());
    }

    @Test
    void shouldReturnEmptyListsWhenNoAccessibleProjectsForDetailQueries() {
        when(projectAccessService.listAccessibleProjectIds(null)).thenReturn(List.of());

        assertEquals(List.of(), service.listProjectProgress());
        assertEquals(List.of(), service.listDeadlineAlerts());
        assertEquals(List.of(), service.listPluginStats());
        verify(dashboardMapper, never()).selectProjectProgress(anyList());
        verify(dashboardMapper, never()).selectDeadlineAlerts(anyList(), any(LocalDate.class), any(LocalDate.class));
        verify(dashboardMapper, never()).selectPluginStats(anyList(), any(Timestamp.class));
    }

    @Test
    void shouldDelegateDeadlineAndPluginStatsQueries() {
        when(projectAccessService.listAccessibleProjectIds(null)).thenReturn(List.of(2L));
        DocProject project = new DocProject();
        project.setId(2L);
        when(projectMapper.selectList(any())).thenReturn(List.of(project));

        DocDeadlineAlertVo alertVo = new DocDeadlineAlertVo();
        alertVo.setProjectId(2L);
        alertVo.setProjectName("演示项目");
        when(dashboardMapper.selectDeadlineAlerts(anyList(), any(LocalDate.class), any(LocalDate.class))).thenReturn(List.of(alertVo));

        DocPluginStatsVo statsVo = new DocPluginStatsVo();
        statsVo.setPluginId("ai-generate");
        statsVo.setPluginName("AI生成插件");
        when(dashboardMapper.selectPluginStats(anyList(), any(Timestamp.class))).thenReturn(List.of(statsVo));

        assertEquals(1, service.listDeadlineAlerts().size());
        assertEquals("演示项目", service.listDeadlineAlerts().get(0).getProjectName());
        assertEquals(1, service.listPluginStats().size());
        assertEquals("AI生成插件", service.listPluginStats().get(0).getPluginName());
    }

    @Test
    void shouldAggregateTodoSummaryFromProjectsAndWorkflow() {
        when(projectAccessService.listAccessibleProjectIds(null)).thenReturn(List.of(1L, 2L));
        DocProject project = new DocProject();
        project.setId(1L);
        when(projectMapper.selectList(any())).thenReturn(List.of(project));
        when(projectMapper.selectCount(any())).thenReturn(1L);
        when(nodeDeadlineMapper.selectCount(any())).thenReturn(2L);
        when(flwTaskService.pageByTaskWait(any(), any())).thenReturn(new org.dromara.common.mybatis.core.page.TableDataInfo<>(List.of(), 5L));
        when(flwTaskService.pageByTaskCopy(any(), any())).thenReturn(new org.dromara.common.mybatis.core.page.TableDataInfo<>(List.of(), 3L));
        when(flwTaskService.pageByTaskFinish(any(), any())).thenReturn(new org.dromara.common.mybatis.core.page.TableDataInfo<>(List.of(), 4L));

        DocTodoSummaryVo summary = service.getTodoSummary();

        assertEquals(1L, summary.getMyProjectCount());
        assertEquals(1L, summary.getActiveProjectCount());
        assertEquals(2L, summary.getOverdueNodeCount());
        assertEquals(5L, summary.getWaitingTaskCount());
        assertEquals(3L, summary.getCopiedTaskCount());
        assertEquals(4L, summary.getFinishedTaskCount());
    }
}
