package org.dromara.docman.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.web.core.BaseController;
import org.dromara.docman.application.service.DocArchiveApplicationService;
import org.dromara.docman.domain.vo.DocArchivePackageVo;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/docman/archive")
public class DocArchiveController extends BaseController {

    private final DocArchiveApplicationService archiveApplicationService;

    /**
     * 为指定项目创建新的归档包。
     *
     * @param projectId 项目ID
     * @return 归档结果
     */
    @SaCheckPermission("docman:archive:execute")
    @Log(title = "项目归档", businessType = BusinessType.UPDATE)
    @PostMapping("/{projectId}")
    public R<DocArchivePackageVo> archive(@PathVariable Long projectId) {
        return R.ok(archiveApplicationService.archive(projectId));
    }

    /**
     * 查询项目最近一次归档结果。
     *
     * @param projectId 项目ID
     * @return 最新归档包
     */
    @SaCheckPermission("docman:archive:query")
    @GetMapping("/{projectId}")
    public R<DocArchivePackageVo> getArchive(@PathVariable Long projectId) {
        return R.ok(archiveApplicationService.getLatest(projectId));
    }

    /**
     * 查询项目历史归档记录。
     *
     * @param projectId 项目ID
     * @return 归档历史列表
     */
    @SaCheckPermission("docman:archive:query")
    @GetMapping("/history/{projectId}")
    public R<List<DocArchivePackageVo>> getArchiveHistory(@PathVariable Long projectId) {
        return R.ok(archiveApplicationService.listHistory(projectId));
    }

    /**
     * 下载指定归档包。
     *
     * @param archiveId 归档包ID
     * @param response  HTTP响应
     */
    @SaCheckPermission("docman:archive:download")
    @GetMapping("/{archiveId}/download")
    public void download(@PathVariable Long archiveId, HttpServletResponse response) {
        archiveApplicationService.downloadArchive(archiveId, response);
    }
}
