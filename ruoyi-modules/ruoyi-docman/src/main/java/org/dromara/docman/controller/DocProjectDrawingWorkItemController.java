package org.dromara.docman.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.web.core.BaseController;
import org.dromara.docman.application.service.DocProjectDrawingWorkItemApplicationService;
import org.dromara.docman.application.service.DocProjectDrawingWorkItemQueryApplicationService;
import org.dromara.docman.domain.bo.DocProjectDrawingWorkItemBo;
import org.dromara.docman.domain.vo.DocProjectDrawingWorkItemVo;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/docman/project/drawing/work-item")
public class DocProjectDrawingWorkItemController extends BaseController {

    private final DocProjectDrawingWorkItemQueryApplicationService queryApplicationService;
    private final DocProjectDrawingWorkItemApplicationService applicationService;

    /**
     * 查询项目下全部图纸工作量映射列表。
     *
     * @param projectId 项目ID
     * @return 图纸工作量映射列表
     */
    @SaCheckPermission("docman:project:query")
    @GetMapping("/project-list")
    public R<List<DocProjectDrawingWorkItemVo>> listByProject(@RequestParam Long projectId) {
        return R.ok(queryApplicationService.listByProject(projectId));
    }

    /**
     * 查询图纸工作量映射列表。
     *
     * @param projectId 项目ID
     * @param drawingId 图纸ID
     * @return 图纸工作量映射列表
     */
    @SaCheckPermission("docman:project:query")
    @GetMapping("/list")
    public R<List<DocProjectDrawingWorkItemVo>> list(@RequestParam Long projectId, @RequestParam Long drawingId) {
        return R.ok(queryApplicationService.listByDrawing(projectId, drawingId));
    }

    /**
     * 保存图纸工作量映射。
     *
     * @param bo 图纸工作量映射参数
     * @return 映射ID
     */
    @SaCheckPermission("docman:project:edit")
    @Log(title = "图纸工作量映射", businessType = BusinessType.INSERT)
    @PostMapping
    public R<Long> save(@Validated @RequestBody DocProjectDrawingWorkItemBo bo) {
        return R.ok(applicationService.save(bo));
    }

    /**
     * 批量删除图纸工作量映射。
     *
     * @param ids 映射ID列表
     * @return 删除结果
     */
    @SaCheckPermission("docman:project:remove")
    @Log(title = "图纸工作量映射", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@PathVariable List<Long> ids) {
        applicationService.delete(ids);
        return R.ok();
    }
}
