package org.dromara.docman.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.web.core.BaseController;
import org.dromara.docman.application.service.DocNodeDeadlineApplicationService;
import org.dromara.docman.application.service.DocNodeDeadlineQueryApplicationService;
import org.dromara.docman.domain.bo.DocNodeDeadlineBo;
import org.dromara.docman.domain.bo.NodeDurationBo;
import org.dromara.docman.domain.vo.DocNodeDeadlineVo;
import org.dromara.docman.domain.vo.FlowNodeDurationVo;
import org.dromara.warm.flow.core.FlowEngine;
import org.dromara.warm.flow.core.entity.Node;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/docman/node/deadline")
public class DocNodeDeadlineController extends BaseController {

    private final DocNodeDeadlineQueryApplicationService nodeDeadlineQueryApplicationService;
    private final DocNodeDeadlineApplicationService nodeDeadlineApplicationService;

    /**
     * 查询项目下所有节点截止日期配置。
     *
     * @param projectId 项目ID
     * @return 节点截止日期列表
     */
    @SaCheckPermission("docman:nodedeadline:query")
    @GetMapping("/list")
    public R<List<DocNodeDeadlineVo>> list(@RequestParam Long projectId) {
        return R.ok(nodeDeadlineQueryApplicationService.listByProject(projectId));
    }

    /**
     * 更新节点截止日期配置。
     *
     * @param bo 节点截止日期变更参数
     * @return 执行结果
     */
    @SaCheckPermission("docman:nodedeadline:edit")
    @Log(title = "节点截止日期修改", businessType = BusinessType.UPDATE)
    @PutMapping
    public R<Void> edit(@Validated @RequestBody DocNodeDeadlineBo bo) {
        nodeDeadlineApplicationService.update(bo);
        return R.ok();
    }

    /**
     * 查询流程定义下的节点及其时限配置。
     *
     * @param definitionId 流程定义ID
     * @return 节点时限列表
     */
    @SaCheckPermission("docman:nodedeadline:query")
    @GetMapping("/nodes")
    public R<List<FlowNodeDurationVo>> listNodes(@RequestParam Long definitionId) {
        List<? extends Node> nodes = FlowEngine.nodeService().getByDefId(definitionId);
        List<FlowNodeDurationVo> voList = nodes.stream().map(node -> {
            FlowNodeDurationVo vo = new FlowNodeDurationVo();
            vo.setNodeId(node.getId());
            vo.setNodeCode(node.getNodeCode());
            vo.setNodeName(node.getNodeName());
            vo.setDurationDays(resolveDurationDays(node.getExt()));
            return vo;
        }).toList();
        return R.ok(voList);
    }

    /**
     * 直接更新 Warm-Flow 节点扩展中的持续天数字段。
     *
     * @param bo 节点时限参数
     * @return 执行结果
     */
    @SaCheckPermission("docman:nodedeadline:edit")
    @Log(title = "节点时限修改", businessType = BusinessType.UPDATE)
    @PutMapping("/node-duration")
    public R<Void> updateNodeDuration(@Validated @RequestBody NodeDurationBo bo) {
        Node node = FlowEngine.nodeService().getById(bo.getNodeId());
        if (node == null) {
            throw new ServiceException("流程节点不存在");
        }
        String ext = node.getExt();
        JSONObject extJson = StrUtil.isBlank(ext) ? new JSONObject() : JSONUtil.parseObj(ext);
        extJson.set("durationDays", bo.getDurationDays());
        node.setExt(extJson.toString());
        FlowEngine.nodeService().updateById(node);
        return R.ok();
    }

    /**
     * 从节点扩展配置中解析持续天数。
     *
     * @param nodeExt 节点扩展JSON
     * @return 持续天数
     */
    private int resolveDurationDays(String nodeExt) {
        if (StrUtil.isBlank(nodeExt)) return 0;
        try {
            JSONObject extJson = JSONUtil.parseObj(nodeExt);
            Integer durationDays = extJson.getInt("durationDays");
            if (durationDays != null) return durationDays;
            JSONObject extra = extJson.getJSONObject("extra");
            return extra == null ? 0 : extra.getInt("durationDays", 0);
        } catch (Exception e) {
            return 0;
        }
    }
}
