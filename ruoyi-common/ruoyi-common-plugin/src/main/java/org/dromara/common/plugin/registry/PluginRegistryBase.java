package org.dromara.common.plugin.registry;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.plugin.annotation.PluginComponent;
import org.dromara.common.plugin.spi.Plugin;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;

/**
 * 通用插件注册表基类。
 *
 * <p>子类只需指定泛型并声明为 Spring Bean：
 * <pre>{@code
 * @Component
 * public class DocumentPluginRegistry extends PluginRegistryBase<PluginContext, PluginResult> {}
 * }</pre>
 *
 * <p>所有被 {@link PluginComponent} 标注且匹配泛型的 Plugin 实现会自动注册。
 *
 * @param <C> 插件上下文类型
 * @param <R> 插件结果类型
 */
@Slf4j
public abstract class PluginRegistryBase<C, R> {

    @Autowired(required = false)
    private List<Plugin<C, R>> plugins;

    private final Map<String, Plugin<C, R>> registry = new LinkedHashMap<>();

    @PostConstruct
    public void init() {
        if (plugins == null || plugins.isEmpty()) {
            log.info("[PluginRegistry] {} 无插件注册", getClass().getSimpleName());
            return;
        }
        for (Plugin<C, R> plugin : plugins) {
            PluginComponent annotation = plugin.getClass().getAnnotation(PluginComponent.class);
            String pluginId = (annotation != null && !annotation.value().isEmpty())
                ? annotation.value() : plugin.getPluginId();
            if (registry.containsKey(pluginId)) {
                log.warn("[PluginRegistry] 插件ID重复，将被覆盖: {}", pluginId);
            }
            registry.put(pluginId, plugin);
            log.info("[PluginRegistry] 注册插件: {} ({})", pluginId, plugin.getPluginName());
        }
        log.info("[PluginRegistry] {} 注册完成，共 {} 个", getClass().getSimpleName(), registry.size());
    }

    public Plugin<C, R> getPlugin(String pluginId) {
        return registry.get(pluginId);
    }

    public Map<String, Plugin<C, R>> getAllPlugins() {
        return Collections.unmodifiableMap(registry);
    }

    public List<Plugin<C, R>> getPluginsByType(String type) {
        return registry.values().stream()
            .filter(p -> type.equals(p.getPluginType()))
            .toList();
    }

    public int size() {
        return registry.size();
    }
}
