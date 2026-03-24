package org.dromara.docman.service;

import org.dromara.docman.domain.vo.DocDashboardOverviewVo;
import org.dromara.docman.domain.vo.DocDeadlineAlertVo;
import org.dromara.docman.domain.vo.DocPluginStatsVo;
import org.dromara.docman.domain.vo.DocProjectProgressVo;
import org.dromara.docman.domain.vo.DocTodoSummaryVo;

import java.util.List;

public interface IDocDashboardService {

    DocDashboardOverviewVo getOverview();

    DocTodoSummaryVo getTodoSummary();

    List<DocProjectProgressVo> listProjectProgress();

    List<DocDeadlineAlertVo> listDeadlineAlerts();

    List<DocPluginStatsVo> listPluginStats();
}
