package org.dromara.docman.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
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

    @SaCheckPermission("docman:archive:execute")
    @Log(title = "项目归档", businessType = BusinessType.UPDATE)
    @PostMapping("/{projectId}")
    public R<DocArchivePackageVo> archive(@PathVariable Long projectId) {
        return R.ok(archiveApplicationService.archive(projectId));
    }

    @SaCheckPermission("docman:archive:query")
    @GetMapping("/{projectId}")
    public R<DocArchivePackageVo> getArchive(@PathVariable Long projectId) {
        return R.ok(archiveApplicationService.getLatest(projectId));
    }

    @SaCheckPermission("docman:archive:query")
    @GetMapping("/history/{projectId}")
    public R<List<DocArchivePackageVo>> getArchiveHistory(@PathVariable Long projectId) {
        return R.ok(archiveApplicationService.listHistory(projectId));
    }
}
