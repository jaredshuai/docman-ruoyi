package org.dromara.docman.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.web.core.BaseController;
import org.dromara.docman.application.service.DocProjectEstimateApplicationService;
import org.dromara.docman.application.service.DocProjectEstimateQueryApplicationService;
import org.dromara.docman.domain.vo.DocProjectEstimateSnapshotVo;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 电信项目估算接口。
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/docman/project")
public class DocProjectEstimateController extends BaseController {

    private final DocProjectEstimateQueryApplicationService queryApplicationService;
    private final DocProjectEstimateApplicationService applicationService;

    @SaCheckPermission("docman:project:query")
    @GetMapping("/{projectId}/estimate/latest")
    public R<DocProjectEstimateSnapshotVo> latest(@PathVariable Long projectId) {
        return R.ok(queryApplicationService.queryLatest(projectId));
    }

    @SaCheckPermission("docman:project:edit")
    @Log(title = "项目初步估算", businessType = BusinessType.UPDATE)
    @PostMapping("/{projectId}/estimate/trigger")
    public R<Void> trigger(@PathVariable Long projectId) {
        applicationService.triggerEstimate(projectId);
        return R.ok();
    }
}
