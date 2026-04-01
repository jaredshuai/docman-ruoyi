package org.dromara.docman.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.web.core.BaseController;
import org.dromara.docman.domain.bo.DocPluginTriggerBo;
import org.dromara.docman.domain.vo.DocPluginExecutionLogVo;
import org.dromara.docman.application.service.DocPluginApplicationService;
import org.dromara.docman.domain.vo.DocPluginInfoVo;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
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

    /**
     * 分页查询插件执行日志（列表不含快照字段）
     */
    @SaCheckPermission("docman:plugin:list")
    @GetMapping("/execution/list")
    public TableDataInfo<DocPluginExecutionLogVo> listExecutionLogs(@RequestParam Long projectId,
                                                                    @RequestParam(required = false) Long processInstanceId,
                                                                    @RequestParam(required = false) String nodeCode,
                                                                    @RequestParam(required = false) String pluginId,
                                                                    PageQuery pageQuery) {
        return pluginApplicationService.listExecutionLogs(projectId, processInstanceId, nodeCode, pluginId, pageQuery);
    }

    /**
     * 查询插件执行日志详情（包含完整快照字段）
     */
    @SaCheckPermission("docman:plugin:list")
    @GetMapping("/execution/{id}")
    public R<DocPluginExecutionLogVo> getExecutionLog(@PathVariable Long id) {
        return R.ok(pluginApplicationService.getExecutionLogById(id));
    }

    /**
     * 手动触发指定流程节点上的插件执行。
     *
     * @param bo 插件触发参数
     * @return 执行结果
     */
    @SaCheckPermission("docman:plugin:trigger")
    @Log(title = "插件手动触发", businessType = BusinessType.OTHER)
    @PostMapping("/execution/trigger")
    public R<Void> triggerExecution(@Validated @RequestBody DocPluginTriggerBo bo) {
        pluginApplicationService.triggerPlugin(bo);
        return R.ok();
    }
}
