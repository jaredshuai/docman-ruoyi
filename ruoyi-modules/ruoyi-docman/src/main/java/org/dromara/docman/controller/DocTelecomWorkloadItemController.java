package org.dromara.docman.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.web.core.BaseController;
import org.dromara.docman.domain.bo.DocTelecomWorkloadItemBo;
import org.dromara.docman.domain.vo.DocTelecomWorkloadItemVo;
import org.dromara.docman.service.IDocTelecomWorkloadItemService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 电信工作量基础维护控制器。
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/docman/workload-item")
public class DocTelecomWorkloadItemController extends BaseController {

    private final IDocTelecomWorkloadItemService workloadItemService;

    @SaCheckPermission("docman:workload-item:list")
    @GetMapping("/list")
    public R<List<DocTelecomWorkloadItemVo>> list() {
        return R.ok(workloadItemService.listAll());
    }

    @SaCheckPermission("docman:workload-item:query")
    @GetMapping("/{id}")
    public R<DocTelecomWorkloadItemVo> getInfo(@PathVariable Long id) {
        return R.ok(workloadItemService.queryById(id));
    }

    @SaCheckPermission("docman:workload-item:edit")
    @Log(title = "工作量基础维护", businessType = BusinessType.INSERT)
    @PostMapping
    public R<Long> save(@Validated @RequestBody DocTelecomWorkloadItemBo bo) {
        return R.ok(workloadItemService.save(bo));
    }

    @SaCheckPermission("docman:workload-item:remove")
    @Log(title = "工作量基础维护", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@PathVariable List<Long> ids) {
        workloadItemService.deleteByIds(ids);
        return R.ok();
    }
}
