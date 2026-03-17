package org.dromara.common.plugin.annotation;

import org.springframework.stereotype.Component;

import java.lang.annotation.*;

/**
 * 通用插件标记注解。标注在 {@link org.dromara.common.plugin.spi.Plugin} 实现上，
 * 框架自动发现并注册到 {@link org.dromara.common.plugin.registry.PluginRegistryBase}。
 *
 * <p>value 为插件 ID，留空则使用 {@code Plugin#getPluginId()} 返回值。
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface PluginComponent {

    String value() default "";
}
