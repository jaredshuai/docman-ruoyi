package org.dromara.docman.service;

import org.dromara.docman.domain.vo.DocDashboardOverviewVo;
import org.dromara.docman.domain.vo.DocDeadlineAlertVo;
import org.dromara.docman.domain.vo.DocPluginStatsVo;
import org.dromara.docman.domain.vo.DocProjectProgressVo;

import java.util.List;

public interface IDocDashboardService {

    DocDashboardOverviewVo getOverview();

    List<DocProjectProgressVo> listProjectProgress();

    List<DocDeadlineAlertVo> listDeadlineAlerts();

    List<DocPluginStatsVo> listPluginStats();
}
