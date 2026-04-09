package org.dromara.docman.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.web.core.BaseController;
import org.dromara.docman.application.service.DocWorkflowTemplateApplicationService;
import org.dromara.docman.application.service.DocWorkflowTemplateQueryApplicationService;
import org.dromara.docman.domain.bo.DocWorkflowTemplateBo;
import org.dromara.docman.domain.vo.DocWorkflowTemplateVo;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 工作流模板控制器
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/docman/workflow-template")
public class DocWorkflowTemplateController extends BaseController {

    private final DocWorkflowTemplateQueryApplicationService queryApplicationService;
    private final DocWorkflowTemplateApplicationService applicationService;

    @SaCheckPermission("docman:process:query")
    @GetMapping("/list")
    public R<List<DocWorkflowTemplateVo>> list(@RequestParam(required = false) String projectTypeCode) {
        return R.ok(queryApplicationService.listByProjectType(projectTypeCode));
    }

    @SaCheckPermission("docman:process:query")
    @GetMapping("/{id}")
    public R<DocWorkflowTemplateVo> getInfo(@PathVariable Long id) {
        return R.ok(queryApplicationService.queryById(id));
    }

    @SaCheckPermission("docman:process:bind")
    @Log(title = "工作流模板", businessType = BusinessType.INSERT)
    @PostMapping
    public R<Long> save(@Validated @RequestBody DocWorkflowTemplateBo bo) {
        return R.ok(applicationService.save(bo));
    }

    @SaCheckPermission("docman:process:bind")
    @Log(title = "工作流模板", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@PathVariable List<Long> ids) {
        applicationService.delete(ids);
        return R.ok();
    }
}
