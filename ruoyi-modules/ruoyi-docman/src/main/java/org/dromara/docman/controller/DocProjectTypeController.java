package org.dromara.docman.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.web.core.BaseController;
import org.dromara.docman.application.service.DocProjectTypeApplicationService;
import org.dromara.docman.application.service.DocProjectTypeQueryApplicationService;
import org.dromara.docman.domain.bo.DocProjectTypeBo;
import org.dromara.docman.domain.vo.DocProjectTypeVo;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 项目类型定义控制器
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/docman/project-type")
public class DocProjectTypeController extends BaseController {

    private final DocProjectTypeQueryApplicationService queryApplicationService;
    private final DocProjectTypeApplicationService applicationService;

    @SaCheckPermission("docman:project:list")
    @GetMapping("/list")
    public R<List<DocProjectTypeVo>> list() {
        return R.ok(queryApplicationService.listAll());
    }

    @SaCheckPermission("docman:project:query")
    @GetMapping("/{id}")
    public R<DocProjectTypeVo> getInfo(@PathVariable Long id) {
        return R.ok(queryApplicationService.queryById(id));
    }

    @SaCheckPermission("docman:project:edit")
    @Log(title = "项目类型定义", businessType = BusinessType.INSERT)
    @PostMapping
    public R<Long> save(@Validated @RequestBody DocProjectTypeBo bo) {
        return R.ok(applicationService.save(bo));
    }

    @SaCheckPermission("docman:project:remove")
    @Log(title = "项目类型定义", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@PathVariable List<Long> ids) {
        applicationService.delete(ids);
        return R.ok();
    }
}
