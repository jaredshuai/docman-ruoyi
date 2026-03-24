package org.dromara.docman.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.web.core.BaseController;
import org.dromara.docman.application.service.DocProcessApplicationService;
import org.dromara.docman.application.service.DocProcessQueryApplicationService;
import org.dromara.docman.domain.vo.DocProcessConfigVo;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/docman/process")
public class DocProcessController extends BaseController {

    private final DocProcessApplicationService processApplicationService;
    private final DocProcessQueryApplicationService processQueryApplicationService;

    @SaCheckPermission("docman:process:bind")
    @Log(title = "流程绑定", businessType = BusinessType.INSERT)
    @PostMapping("/bind")
    public R<Void> bind(@RequestParam Long projectId, @RequestParam Long definitionId) {
        processApplicationService.bind(projectId, definitionId);
        return R.ok();
    }

    @SaCheckPermission("docman:process:start")
    @Log(title = "流程启动", businessType = BusinessType.UPDATE)
    @PostMapping("/start/{projectId}")
    public R<Long> start(@PathVariable Long projectId) {
        return R.ok(processApplicationService.start(projectId));
    }

    @SaCheckPermission("docman:process:query")
    @GetMapping("/{projectId}")
    public R<DocProcessConfigVo> getConfig(@PathVariable Long projectId) {
        return R.ok(processQueryApplicationService.getConfig(projectId));
    }

    @SaCheckPermission("docman:process:query")
    @GetMapping("/definitions")
    public R<List<Map<String, Object>>> listDefinitions() {
        return R.ok(processApplicationService.listDefinitions());
    }
}
