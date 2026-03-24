package org.dromara.docman.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.web.core.BaseController;
import org.dromara.docman.domain.vo.DocDashboardOverviewVo;
import org.dromara.docman.domain.vo.DocDeadlineAlertVo;
import org.dromara.docman.domain.vo.DocPluginStatsVo;
import org.dromara.docman.domain.vo.DocProjectProgressVo;
import org.dromara.docman.service.IDocDashboardService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/docman/dashboard")
public class DocDashboardController extends BaseController {

    private final IDocDashboardService dashboardService;

    @SaCheckPermission("docman:project:list")
    @GetMapping("/overview")
    public R<DocDashboardOverviewVo> overview() {
        return R.ok(dashboardService.getOverview());
    }

    @SaCheckPermission("docman:project:list")
    @GetMapping("/project-progress")
    public R<List<DocProjectProgressVo>> projectProgress() {
        return R.ok(dashboardService.listProjectProgress());
    }

    @SaCheckPermission("docman:project:list")
    @GetMapping("/deadline-alert")
    public R<List<DocDeadlineAlertVo>> deadlineAlert() {
        return R.ok(dashboardService.listDeadlineAlerts());
    }

    @SaCheckPermission("docman:project:list")
    @GetMapping("/plugin-stats")
    public R<List<DocPluginStatsVo>> pluginStats() {
        return R.ok(dashboardService.listPluginStats());
    }
}
