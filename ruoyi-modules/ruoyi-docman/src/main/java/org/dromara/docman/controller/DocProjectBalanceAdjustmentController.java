package org.dromara.docman.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.web.core.BaseController;
import org.dromara.docman.application.service.DocProjectBalanceAdjustmentApplicationService;
import org.dromara.docman.application.service.DocProjectBalanceAdjustmentQueryApplicationService;
import org.dromara.docman.domain.bo.DocProjectBalanceAdjustmentBo;
import org.dromara.docman.domain.vo.DocProjectBalanceAdjustmentVo;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 项目平料接口。
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/docman/project")
public class DocProjectBalanceAdjustmentController extends BaseController {

    private final DocProjectBalanceAdjustmentQueryApplicationService queryApplicationService;
    private final DocProjectBalanceAdjustmentApplicationService applicationService;

    /**
     * 查询项目最新平料结果。
     *
     * @param projectId 项目ID
     * @return 最新平料结果
     */
    @SaCheckPermission("docman:project:query")
    @GetMapping("/{projectId}/balance/latest")
    public R<DocProjectBalanceAdjustmentVo> latest(@PathVariable Long projectId) {
        return R.ok(queryApplicationService.queryLatest(projectId));
    }

    /**
     * 保存项目平料结果。
     *
     * @param projectId 项目ID
     * @param bo        平料参数
     * @return 平料记录ID
     */
    @SaCheckPermission("docman:project:edit")
    @Log(title = "项目平料", businessType = BusinessType.UPDATE)
    @PostMapping("/{projectId}/balance")
    public R<Long> save(@PathVariable Long projectId, @Validated @RequestBody DocProjectBalanceAdjustmentBo bo) {
        bo.setProjectId(projectId);
        return R.ok(applicationService.save(bo));
    }
}
