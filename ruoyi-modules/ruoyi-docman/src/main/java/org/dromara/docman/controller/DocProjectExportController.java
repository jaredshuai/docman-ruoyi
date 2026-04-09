package org.dromara.docman.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.web.core.BaseController;
import org.dromara.docman.application.service.DocProjectExportApplicationService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 项目文本导出接口。
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/docman/project")
public class DocProjectExportController extends BaseController {

    private final DocProjectExportApplicationService applicationService;

    /**
     * 手动触发项目文本导出。
     *
     * @param projectId 项目ID
     * @return 执行结果
     */
    @SaCheckPermission("docman:project:edit")
    @Log(title = "项目文本导出", businessType = BusinessType.UPDATE)
    @PostMapping("/{projectId}/export/trigger")
    public R<Void> trigger(@PathVariable Long projectId) {
        applicationService.triggerExportText(projectId);
        return R.ok();
    }
}
