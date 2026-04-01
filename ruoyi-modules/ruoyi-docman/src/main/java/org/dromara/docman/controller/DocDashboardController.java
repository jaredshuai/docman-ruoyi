package org.dromara.docman.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.web.core.BaseController;
import org.dromara.docman.domain.vo.DocDashboardOverviewVo;
import org.dromara.docman.domain.vo.DocDeadlineAlertVo;
import org.dromara.docman.domain.vo.DocPluginStatsVo;
import org.dromara.docman.domain.vo.DocProjectProgressVo;
import org.dromara.docman.domain.vo.DocTodoSummaryVo;
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

    /**
     * 获取文档模块看板总览。
     *
     * @return 看板总览
     */
    @SaCheckPermission("docman:project:list")
    @GetMapping("/overview")
    public R<DocDashboardOverviewVo> overview() {
        return R.ok(dashboardService.getOverview());
    }

    /**
     * 获取当前用户待办摘要。
     *
     * @return 待办统计
     */
    @SaCheckPermission("docman:dashboard:todo-summary")
    @GetMapping("/todo-summary")
    public R<DocTodoSummaryVo> todoSummary() {
        return R.ok(dashboardService.getTodoSummary());
    }

    /**
     * 获取项目进度列表。
     *
     * @return 项目进度
     */
    @SaCheckPermission("docman:project:list")
    @GetMapping("/project-progress")
    public R<List<DocProjectProgressVo>> projectProgress() {
        return R.ok(dashboardService.listProjectProgress());
    }

    /**
     * 获取节点超期预警列表。
     *
     * @return 超期预警
     */
    @SaCheckPermission("docman:project:list")
    @GetMapping("/deadline-alert")
    public R<List<DocDeadlineAlertVo>> deadlineAlert() {
        return R.ok(dashboardService.listDeadlineAlerts());
    }

    /**
     * 获取插件执行统计。
     *
     * @return 插件统计结果
     */
    @SaCheckPermission("docman:project:list")
    @GetMapping("/plugin-stats")
    public R<List<DocPluginStatsVo>> pluginStats() {
        return R.ok(dashboardService.listPluginStats());
    }
}
