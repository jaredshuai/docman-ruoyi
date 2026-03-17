package org.dromara.common.core.domain.event;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 标准化流程节点完成事件。
 *
 * <p>由 workflow 模块发布，业务模块按需订阅，避免直接依赖 Warm-Flow 运行时对象。</p>
 *
 * <p>nodeExt 是节点扩展 JSON 字符串，可通过 {@link #getParsedExt()} 获取结构化解析结果。
 * 建议由框架统一解析，业务模块不再自行处理 JSON。</p>
 */
@Data
public class WorkflowNodeFinishedEvent implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String tenantId;

    private String flowCode;

    private Long instanceId;

    private String businessId;

    private Integer nodeType;

    private String nodeCode;

    private String nodeName;

    /** 节点扩展 JSON 原始字符串 */
    private String nodeExt;

    private String status;

    private Map<String, Object> params;

    /** 结构化解析后的 ext 对象（由框架层填充） */
    private transient NodeExtPayload parsedExt;

    /**
     * 获取结构化 ext 解析结果。若未解析则返回空对象。
     */
    public NodeExtPayload getParsedExt() {
        return parsedExt != null ? parsedExt : NodeExtPayload.EMPTY;
    }

    /**
     * 节点扩展信息结构化载体。
     */
    @Data
    public static class NodeExtPayload implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        public static final NodeExtPayload EMPTY = new NodeExtPayload();

        private String archiveFolderName;

        private List<PluginBinding> plugins;

        private Map<String, Object> extra;

        public List<PluginBinding> getPlugins() {
            return plugins != null ? plugins : Collections.emptyList();
        }

        public Map<String, Object> getExtra() {
            return extra != null ? extra : Collections.emptyMap();
        }
    }

    /**
     * 节点上配置的插件绑定信息。
     */
    @Data
    public static class PluginBinding implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private String pluginId;

        private Map<String, Object> config;

        public Map<String, Object> getConfig() {
            return config != null ? config : Collections.emptyMap();
        }
    }
}
