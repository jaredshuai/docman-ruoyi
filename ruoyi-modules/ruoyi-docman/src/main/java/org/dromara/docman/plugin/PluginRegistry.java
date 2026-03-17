package org.dromara.docman.plugin;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.dromara.docman.plugin.annotation.DocPlugin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 插件运行时注册表。
 * <p>DocumentPlugin 为 SPI，PluginRegistry / PluginExecutor 为 Runtime。</p>
 */
@Slf4j
@Component
public class PluginRegistry {

    private final List<DocumentPlugin> plugins;

    private final Map<String, DocumentPlugin> registry = new LinkedHashMap<>();

    public PluginRegistry(@Autowired(required = false) List<DocumentPlugin> plugins) {
        this.plugins = plugins == null ? Collections.emptyList() : plugins;
    }

    @PostConstruct
    public void init() {
        for (DocumentPlugin plugin : plugins) {
            DocPlugin annotation = plugin.getClass().getAnnotation(DocPlugin.class);
            String pluginId = annotation != null ? annotation.value() : plugin.getPluginId();
            if (registry.containsKey(pluginId)) {
                log.warn("插件ID重复，将被覆盖: {}", pluginId);
            }
            registry.put(pluginId, plugin);
            log.info("注册文档插件: {} ({})", pluginId, plugin.getPluginName());
        }
        log.info("文档插件注册完成，共 {} 个", registry.size());
    }

    public DocumentPlugin getPlugin(String pluginId) {
        return registry.get(pluginId);
    }

    public Map<String, DocumentPlugin> getAllPlugins() {
        return Collections.unmodifiableMap(registry);
    }

    public List<DocumentPlugin> getPluginsByType(PluginType type) {
        return registry.values().stream()
            .filter(p -> p.getPluginType() == type)
            .toList();
    }
}
