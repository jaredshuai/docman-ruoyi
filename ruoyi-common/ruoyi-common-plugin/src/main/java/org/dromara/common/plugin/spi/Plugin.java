package org.dromara.common.plugin.spi;

import java.util.List;

/**
 * 通用插件 SPI 接口。
 *
 * <p>泛型参数：
 * <ul>
 *   <li>{@code C} — 插件执行上下文类型</li>
 *   <li>{@code R} — 插件执行结果类型</li>
 * </ul>
 *
 * <p>业务模块通过继承此接口定义自己的插件契约，
 * 例如 {@code DocumentPlugin extends Plugin<PluginContext, PluginResult>}。
 *
 * @param <C> 上下文
 * @param <R> 结果
 */
public interface Plugin<C, R> {

    String getPluginId();

    String getPluginName();

    String getPluginType();

    List<PluginFieldDef> getInputFields();

    List<PluginFieldDef> getOutputFields();

    R execute(C context);
}
