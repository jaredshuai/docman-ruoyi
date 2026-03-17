package org.dromara.docman.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.web.core.BaseController;
import org.dromara.docman.domain.vo.DocPluginExecutionLogVo;
import org.dromara.docman.application.service.DocPluginApplicationService;
import org.dromara.docman.domain.vo.DocPluginInfoVo;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/docman/plugin")
public class DocPluginController extends BaseController {

    private final DocPluginApplicationService pluginApplicationService;

    /**
     * 查询已注册的插件列表
     */
    @SaCheckPermission("docman:plugin:list")
    @GetMapping("/list")
    public R<List<DocPluginInfoVo>> list() {
        return R.ok(pluginApplicationService.listPlugins());
    }

    @SaCheckPermission("docman:plugin:list")
    @GetMapping("/execution/list")
    public TableDataInfo<DocPluginExecutionLogVo> listExecutionLogs(@RequestParam Long projectId,
                                                                    @RequestParam(required = false) Long processInstanceId,
                                                                    @RequestParam(required = false) String nodeCode,
                                                                    @RequestParam(required = false) String pluginId,
                                                                    PageQuery pageQuery) {
        return pluginApplicationService.listExecutionLogs(projectId, processInstanceId, nodeCode, pluginId, pageQuery);
    }
}
