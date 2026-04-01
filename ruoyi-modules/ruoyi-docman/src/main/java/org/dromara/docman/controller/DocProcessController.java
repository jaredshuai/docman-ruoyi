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

    /**
     * 为项目绑定 Warm-Flow 流程定义。
     *
     * @param projectId    项目ID
     * @param definitionId 流程定义ID
     * @return 执行结果
     */
    @SaCheckPermission("docman:process:bind")
    @Log(title = "流程绑定", businessType = BusinessType.INSERT)
    @PostMapping("/bind")
    public R<Void> bind(@RequestParam Long projectId, @RequestParam Long definitionId) {
        processApplicationService.bind(projectId, definitionId);
        return R.ok();
    }

    /**
     * 启动项目已绑定的流程实例。
     *
     * @param projectId 项目ID
     * @return 流程实例ID
     */
    @SaCheckPermission("docman:process:start")
    @Log(title = "流程启动", businessType = BusinessType.UPDATE)
    @PostMapping("/start/{projectId}")
    public R<Long> start(@PathVariable Long projectId) {
        return R.ok(processApplicationService.start(projectId));
    }

    /**
     * 查询项目当前流程配置。
     *
     * @param projectId 项目ID
     * @return 流程配置
     */
    @SaCheckPermission("docman:process:query")
    @GetMapping("/{projectId}")
    public R<DocProcessConfigVo> getConfig(@PathVariable Long projectId) {
        return R.ok(processQueryApplicationService.getConfig(projectId));
    }

    /**
     * 查询当前可用的流程定义列表。
     *
     * @return 流程定义摘要
     */
    @SaCheckPermission("docman:process:query")
    @GetMapping("/definitions")
    public R<List<Map<String, Object>>> listDefinitions() {
        return R.ok(processApplicationService.listDefinitions());
    }
}
