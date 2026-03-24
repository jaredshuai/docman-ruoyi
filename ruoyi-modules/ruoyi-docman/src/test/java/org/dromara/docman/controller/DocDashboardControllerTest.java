package org.dromara.docman.controller;

import org.dromara.common.core.domain.R;
import org.dromara.docman.domain.vo.DocDashboardOverviewVo;
import org.dromara.docman.domain.vo.DocDeadlineAlertVo;
import org.dromara.docman.domain.vo.DocPluginStatsVo;
import org.dromara.docman.domain.vo.DocProjectProgressVo;
import org.dromara.docman.domain.vo.DocTodoSummaryVo;
import org.dromara.docman.service.IDocDashboardService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("dev")
@Tag("prod")
@Tag("local")
class DocDashboardControllerTest {

    @Mock
    private IDocDashboardService dashboardService;

    @Test
    void shouldReturnOverviewFromService() {
        DocDashboardController controller = new DocDashboardController(dashboardService);
        DocDashboardOverviewVo expected = new DocDashboardOverviewVo();
        expected.setTotalProjects(3L);
        when(dashboardService.getOverview()).thenReturn(expected);

        R<DocDashboardOverviewVo> result = controller.overview();

        assertEquals(R.SUCCESS, result.getCode());
        assertEquals(expected, result.getData());
        verify(dashboardService).getOverview();
    }

    @Test
    void shouldReturnProjectProgressListFromService() {
        DocDashboardController controller = new DocDashboardController(dashboardService);
        DocProjectProgressVo vo = new DocProjectProgressVo();
        vo.setProjectName("演示项目");
        when(dashboardService.listProjectProgress()).thenReturn(List.of(vo));

        R<List<DocProjectProgressVo>> result = controller.projectProgress();

        assertEquals(R.SUCCESS, result.getCode());
        assertEquals(1, result.getData().size());
        assertEquals("演示项目", result.getData().get(0).getProjectName());
    }

    @Test
    void shouldReturnDeadlineAlertsFromService() {
        DocDashboardController controller = new DocDashboardController(dashboardService);
        DocDeadlineAlertVo vo = new DocDeadlineAlertVo();
        vo.setProjectName("项目A");
        when(dashboardService.listDeadlineAlerts()).thenReturn(List.of(vo));

        R<List<DocDeadlineAlertVo>> result = controller.deadlineAlert();

        assertEquals(R.SUCCESS, result.getCode());
        assertEquals("项目A", result.getData().get(0).getProjectName());
    }

    @Test
    void shouldReturnPluginStatsFromService() {
        DocDashboardController controller = new DocDashboardController(dashboardService);
        DocPluginStatsVo vo = new DocPluginStatsVo();
        vo.setPluginName("AI生成插件");
        when(dashboardService.listPluginStats()).thenReturn(List.of(vo));

        R<List<DocPluginStatsVo>> result = controller.pluginStats();

        assertEquals(R.SUCCESS, result.getCode());
        assertEquals("AI生成插件", result.getData().get(0).getPluginName());
    }

    @Test
    void shouldReturnTodoSummaryFromService() {
        DocDashboardController controller = new DocDashboardController(dashboardService);
        DocTodoSummaryVo summary = new DocTodoSummaryVo();
        summary.setWaitingTaskCount(6L);
        when(dashboardService.getTodoSummary()).thenReturn(summary);

        R<DocTodoSummaryVo> result = controller.todoSummary();

        assertEquals(R.SUCCESS, result.getCode());
        assertEquals(6L, result.getData().getWaitingTaskCount());
    }
}
