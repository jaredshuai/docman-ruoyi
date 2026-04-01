package org.dromara.docman.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.web.core.BaseController;
import org.dromara.docman.application.service.DocProjectMemberApplicationService;
import org.dromara.docman.application.service.DocProjectMemberQueryApplicationService;
import org.dromara.docman.domain.bo.DocProjectMemberBo;
import org.dromara.docman.domain.vo.DocProjectMemberVo;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/docman/project/{projectId}/member")
public class DocProjectMemberController extends BaseController {

    private final DocProjectMemberApplicationService projectMemberApplicationService;
    private final DocProjectMemberQueryApplicationService projectMemberQueryApplicationService;

    /**
     * 查询项目成员列表。
     *
     * @param projectId 项目ID
     * @return 项目成员列表
     */
    @SaCheckPermission("docman:project:query")
    @GetMapping
    public R<List<DocProjectMemberVo>> list(@PathVariable Long projectId) {
        return R.ok(projectMemberQueryApplicationService.list(projectId));
    }

    /**
     * 向项目中新增成员。
     *
     * @param projectId 项目ID
     * @param bo        成员参数
     * @return 执行结果
     */
    @SaCheckPermission("docman:project:edit")
    @Log(title = "项目成员", businessType = BusinessType.INSERT)
    @PostMapping
    public R<Void> add(@PathVariable Long projectId, @Validated @RequestBody DocProjectMemberBo bo) {
        projectMemberApplicationService.add(projectId, bo);
        return R.ok();
    }

    /**
     * 从项目中移除指定成员。
     *
     * @param projectId 项目ID
     * @param userId    用户ID
     * @return 执行结果
     */
    @SaCheckPermission("docman:project:edit")
    @Log(title = "项目成员", businessType = BusinessType.DELETE)
    @DeleteMapping("/{userId}")
    public R<Void> remove(@PathVariable Long projectId, @PathVariable Long userId) {
        projectMemberApplicationService.remove(projectId, userId);
        return R.ok();
    }
}
