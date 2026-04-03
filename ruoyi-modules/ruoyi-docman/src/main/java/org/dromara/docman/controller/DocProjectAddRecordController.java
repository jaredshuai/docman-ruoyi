package org.dromara.docman.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.web.core.BaseController;
import org.dromara.docman.application.service.DocProjectAddRecordApplicationService;
import org.dromara.docman.application.service.DocProjectAddRecordQueryApplicationService;
import org.dromara.docman.domain.bo.DocProjectAddRecordBo;
import org.dromara.docman.domain.vo.DocProjectAddRecordVo;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 项目工作量记录控制器
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/docman/project/add-record")
public class DocProjectAddRecordController extends BaseController {

    private final DocProjectAddRecordQueryApplicationService queryApplicationService;
    private final DocProjectAddRecordApplicationService applicationService;

    /**
     * 查询项目的工作量记录列表（含详情）
     *
     * @param projectId 项目 ID
     * @return 工作量记录列表
     */
    @SaCheckPermission("docman:project:query")
    @GetMapping("/list")
    public R<List<DocProjectAddRecordVo>> list(@RequestParam Long projectId) {
        return R.ok(queryApplicationService.listByProject(projectId));
    }

    /**
     * 查询工作量记录详情
     *
     * @param id 工作量记录 ID
     * @return 工作量记录详情
     */
    @SaCheckPermission("docman:project:query")
    @GetMapping("/{id}")
    public R<DocProjectAddRecordVo> getInfo(@PathVariable Long id) {
        return R.ok(queryApplicationService.queryById(id));
    }

    /**
     * 保存工作量记录（新增或修改）
     *
     * @param bo 工作量记录参数
     * @return 工作量记录 ID
     */
    @SaCheckPermission("docman:project:edit")
    @Log(title = "工作量记录管理", businessType = BusinessType.INSERT)
    @PostMapping
    public R<Long> add(@Validated @RequestBody DocProjectAddRecordBo bo) {
        return R.ok(applicationService.save(bo));
    }

    /**
     * 删除工作量记录
     *
     * @param ids 工作量记录 ID 列表
     * @return 执行结果
     */
    @SaCheckPermission("docman:project:remove")
    @Log(title = "工作量记录管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@PathVariable List<Long> ids) {
        applicationService.delete(ids);
        return R.ok();
    }
}
