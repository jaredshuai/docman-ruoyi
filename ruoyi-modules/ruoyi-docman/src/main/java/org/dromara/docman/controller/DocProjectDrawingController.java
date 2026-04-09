package org.dromara.docman.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.web.core.BaseController;
import org.dromara.docman.application.service.DocProjectDrawingApplicationService;
import org.dromara.docman.application.service.DocProjectDrawingQueryApplicationService;
import org.dromara.docman.domain.bo.DocProjectDrawingBo;
import org.dromara.docman.domain.vo.DocProjectDrawingVo;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/docman/project/drawing")
public class DocProjectDrawingController extends BaseController {

    private final DocProjectDrawingQueryApplicationService queryApplicationService;
    private final DocProjectDrawingApplicationService applicationService;

    @SaCheckPermission("docman:project:query")
    @GetMapping("/list")
    public R<List<DocProjectDrawingVo>> list(@RequestParam Long projectId) {
        return R.ok(queryApplicationService.listByProject(projectId));
    }

    @SaCheckPermission("docman:project:query")
    @GetMapping("/{id}")
    public R<DocProjectDrawingVo> getInfo(@PathVariable Long id) {
        return R.ok(queryApplicationService.queryById(id));
    }

    @SaCheckPermission("docman:project:edit")
    @Log(title = "项目图纸录入", businessType = BusinessType.INSERT)
    @PostMapping
    public R<Long> save(@Validated @RequestBody DocProjectDrawingBo bo) {
        return R.ok(applicationService.save(bo));
    }

    @SaCheckPermission("docman:project:remove")
    @Log(title = "项目图纸录入", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@PathVariable List<Long> ids) {
        applicationService.delete(ids);
        return R.ok();
    }
}
