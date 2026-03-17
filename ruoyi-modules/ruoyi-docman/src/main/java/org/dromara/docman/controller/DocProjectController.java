package org.dromara.docman.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.web.core.BaseController;
import org.dromara.docman.application.service.DocProjectApplicationService;
import org.dromara.docman.application.service.DocProjectQueryApplicationService;
import org.dromara.docman.domain.bo.DocProjectBo;
import org.dromara.docman.domain.vo.DocProjectVo;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/docman/project")
public class DocProjectController extends BaseController {

    private final DocProjectApplicationService projectApplicationService;
    private final DocProjectQueryApplicationService projectQueryApplicationService;

    @SaCheckPermission("docman:project:list")
    @GetMapping("/list")
    public TableDataInfo<DocProjectVo> list(DocProjectBo bo, PageQuery pageQuery) {
        return projectQueryApplicationService.list(bo, pageQuery);
    }

    @SaCheckPermission("docman:project:query")
    @GetMapping("/{id}")
    public R<DocProjectVo> getInfo(@PathVariable Long id) {
        return R.ok(projectQueryApplicationService.getById(id));
    }

    @SaCheckPermission("docman:project:add")
    @Log(title = "项目管理", businessType = BusinessType.INSERT)
    @PostMapping
    public R<Long> add(@Validated @RequestBody DocProjectBo bo) {
        return R.ok(projectApplicationService.create(bo));
    }

    @SaCheckPermission("docman:project:edit")
    @Log(title = "项目管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public R<Void> edit(@Validated @RequestBody DocProjectBo bo) {
        projectApplicationService.update(bo);
        return R.ok();
    }

    @SaCheckPermission("docman:project:remove")
    @Log(title = "项目管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@PathVariable List<Long> ids) {
        projectApplicationService.delete(ids);
        return R.ok();
    }
}
