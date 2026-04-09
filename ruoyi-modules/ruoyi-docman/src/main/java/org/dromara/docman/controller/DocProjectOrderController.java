package org.dromara.docman.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.web.core.BaseController;
import org.dromara.docman.application.service.DocProjectOrderApplicationService;
import org.dromara.docman.application.service.DocProjectOrderQueryApplicationService;
import org.dromara.docman.domain.bo.DocProjectOrderBo;
import org.dromara.docman.domain.vo.DocProjectOrderVo;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 项目签证单控制器
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/docman/project/order")
public class DocProjectOrderController extends BaseController {

    private final DocProjectOrderQueryApplicationService queryApplicationService;
    private final DocProjectOrderApplicationService applicationService;

    /**
     * 查询项目的签证单列表
     *
     * @param projectId 项目ID
     * @return 签证单列表
     */
    @SaCheckPermission("docman:project:query")
    @GetMapping("/list")
    public R<List<DocProjectOrderVo>> list(@RequestParam Long projectId) {
        return R.ok(queryApplicationService.listByProject(projectId));
    }

    /**
     * 查询签证单详情
     *
     * @param id 签证单ID
     * @return 签证单详情
     */
    @SaCheckPermission("docman:project:query")
    @GetMapping("/{id}")
    public R<DocProjectOrderVo> getInfo(@PathVariable Long id) {
        return R.ok(queryApplicationService.getById(id));
    }

    /**
     * 保存签证单（新增或修改）
     *
     * @param bo 签证单参数
     * @return 签证单ID
     */
    @SaCheckPermission("docman:project:edit")
    @Log(title = "项目签证单管理", businessType = BusinessType.INSERT)
    @PostMapping
    public R<Long> add(@Validated @RequestBody DocProjectOrderBo bo) {
        return R.ok(applicationService.save(bo));
    }

    /**
     * 删除签证单
     *
     * @param ids 签证单ID列表
     * @return 执行结果
     */
    @SaCheckPermission("docman:project:remove")
    @Log(title = "项目签证单管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@PathVariable List<Long> ids) {
        applicationService.delete(ids);
        return R.ok();
    }
}
