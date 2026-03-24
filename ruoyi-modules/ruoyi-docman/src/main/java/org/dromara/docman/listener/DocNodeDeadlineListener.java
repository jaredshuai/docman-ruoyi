package org.dromara.docman.listener;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.domain.event.ProcessTaskEvent;
import org.dromara.docman.domain.entity.DocProcessConfig;
import org.dromara.docman.service.IDocNodeDeadlineService;
import org.dromara.docman.service.IDocProcessConfigService;
import org.dromara.warm.flow.core.entity.Node;
import org.dromara.warm.flow.core.service.NodeService;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DocNodeDeadlineListener {

    private final IDocNodeDeadlineService nodeDeadlineService;
    private final IDocProcessConfigService processConfigService;
    private final NodeService nodeService;

    @EventListener
    public void onTaskCreated(ProcessTaskEvent event) {
        try {
            Long instanceId = event.getInstanceId();
            String nodeCode = event.getNodeCode();
            if (instanceId == null || StrUtil.isBlank(nodeCode)) {
                return;
            }

            DocProcessConfig config = processConfigService.queryByInstanceId(instanceId);
            if (config == null) {
                return;
            }

            Node node = nodeService.getByDefIdAndNodeCode(config.getDefinitionId(), nodeCode);
            if (node == null || StrUtil.isBlank(node.getExt())) {
                return;
            }

            int durationDays = resolveDurationDays(node.getExt());
            if (durationDays <= 0) {
                return;
            }

            nodeDeadlineService.createDeadline(instanceId, nodeCode, config.getProjectId(), durationDays);
        } catch (Exception e) {
            log.error("[DocNodeDeadlineListener] 处理节点进入事件异常, instanceId={}, nodeCode={}",
                      event.getInstanceId(), event.getNodeCode(), e);
        }
    }

    private int resolveDurationDays(String nodeExt) {
        try {
            JSONObject extJson = JSONUtil.parseObj(nodeExt);
            Integer durationDays = extJson.getInt("durationDays");
            if (durationDays != null) {
                return durationDays;
            }
            JSONObject extra = extJson.getJSONObject("extra");
            return extra == null ? 0 : extra.getInt("durationDays", 0);
        } catch (Exception e) {
            log.warn("解析节点时限失败: {}", e.getMessage());
            return 0;
        }
    }
}
