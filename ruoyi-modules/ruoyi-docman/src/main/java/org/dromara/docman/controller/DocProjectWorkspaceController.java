package org.dromara.docman.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.web.core.BaseController;
import org.dromara.docman.application.service.DocProjectWorkspaceApplicationService;
import org.dromara.docman.domain.bo.DocProjectAdvanceNodeBo;
import org.dromara.docman.domain.bo.DocProjectNodeTaskCompleteBo;
import org.dromara.docman.domain.vo.DocProjectWorkspaceVo;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/docman/project")
public class DocProjectWorkspaceController extends BaseController {

    private final DocProjectWorkspaceApplicationService workspaceApplicationService;

    @SaCheckPermission("docman:project:query")
    @GetMapping("/{projectId}/workspace")
    public R<DocProjectWorkspaceVo> getWorkspace(@PathVariable Long projectId) {
        return R.ok(workspaceApplicationService.getWorkspace(projectId));
    }

    @SaCheckPermission("docman:project:edit")
    @Log(title = "节点事项完成", businessType = BusinessType.UPDATE)
    @PostMapping("/{projectId}/node-task/{taskRuntimeId}/complete")
    public R<Void> completeTask(@PathVariable Long projectId,
                                @PathVariable Long taskRuntimeId,
                                @RequestBody(required = false) DocProjectNodeTaskCompleteBo bo) {
        workspaceApplicationService.completeTask(projectId, taskRuntimeId, bo == null ? new DocProjectNodeTaskCompleteBo() : bo);
        return R.ok();
    }

    @SaCheckPermission("docman:plugin:trigger")
    @Log(title = "工作台触发插件", businessType = BusinessType.UPDATE)
    @PostMapping("/{projectId}/node-task/{taskRuntimeId}/trigger-plugins")
    public R<Void> triggerTaskPlugins(@PathVariable Long projectId, @PathVariable Long taskRuntimeId) {
        workspaceApplicationService.triggerTaskPlugins(projectId, taskRuntimeId);
        return R.ok();
    }

    @SaCheckPermission("docman:project:edit")
    @Log(title = "项目节点推进", businessType = BusinessType.UPDATE)
    @PostMapping("/{projectId}/node/advance")
    public R<Void> advanceNode(@PathVariable Long projectId, @RequestBody DocProjectAdvanceNodeBo bo) {
        bo.setProjectId(projectId);
        workspaceApplicationService.advanceNode(bo);
        return R.ok();
    }
}
