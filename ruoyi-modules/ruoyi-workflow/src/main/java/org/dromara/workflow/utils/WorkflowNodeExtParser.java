package org.dromara.workflow.utils;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.domain.event.WorkflowNodeFinishedEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 工作流节点 ext JSON 结构化解析器。
 *
 * <p>将节点扩展 JSON 字符串解析为 {@link WorkflowNodeFinishedEvent.NodeExtPayload}，
 * 业务模块不再需要自行解析 JSON。
 *
 * <p>放在 workflow 模块，因为 JSON 解析依赖（hutool-json）在此模块可用。
 * 解析结果通过框架事件传递到业务模块。
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class WorkflowNodeExtParser {

    /**
     * 解析 nodeExt JSON 字符串，返回结构化载体。解析失败则返回空对象。
     */
    public static WorkflowNodeFinishedEvent.NodeExtPayload parse(String nodeExt) {
        if (StrUtil.isBlank(nodeExt)) {
            return WorkflowNodeFinishedEvent.NodeExtPayload.EMPTY;
        }
        try {
            JSONObject extJson = JSONUtil.parseObj(nodeExt);
            WorkflowNodeFinishedEvent.NodeExtPayload payload = new WorkflowNodeFinishedEvent.NodeExtPayload();
            payload.setArchiveFolderName(extJson.getStr("archiveFolderName", ""));
            payload.setPlugins(parsePlugins(extJson.getJSONArray("plugins")));

            @SuppressWarnings("unchecked")
            Map<String, Object> extra = (Map<String, Object>) extJson.get("extra");
            payload.setExtra(extra);
            return payload;
        } catch (Exception e) {
            log.warn("[WorkflowNodeExtParser] 解析节点ext失败: {}", e.getMessage());
            return WorkflowNodeFinishedEvent.NodeExtPayload.EMPTY;
        }
    }

    /**
     * 解析并填充事件的 parsedExt 字段。
     */
    public static void enrich(WorkflowNodeFinishedEvent event) {
        if (event != null) {
            event.setParsedExt(parse(event.getNodeExt()));
        }
    }

    private static List<WorkflowNodeFinishedEvent.PluginBinding> parsePlugins(JSONArray pluginsArray) {
        if (pluginsArray == null || pluginsArray.isEmpty()) {
            return List.of();
        }
        List<WorkflowNodeFinishedEvent.PluginBinding> bindings = new ArrayList<>(pluginsArray.size());
        for (int i = 0; i < pluginsArray.size(); i++) {
            JSONObject pluginJson = pluginsArray.getJSONObject(i);
            WorkflowNodeFinishedEvent.PluginBinding binding = new WorkflowNodeFinishedEvent.PluginBinding();
            binding.setPluginId(pluginJson.getStr("pluginId"));
            @SuppressWarnings("unchecked")
            Map<String, Object> config = (Map<String, Object>) pluginJson.get("config");
            binding.setConfig(config);
            bindings.add(binding);
        }
        return bindings;
    }
}
