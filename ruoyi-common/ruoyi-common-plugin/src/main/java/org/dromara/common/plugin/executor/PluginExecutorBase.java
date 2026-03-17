package org.dromara.common.plugin.executor;

import lombok.extern.slf4j.Slf4j;
import org.dromara.common.plugin.spi.Plugin;

import java.util.function.BiConsumer;

/**
 * 通用插件执行器基类。
 *
 * <p>提供异常捕获、耗时统计和可选的回调扩展点（执行前/执行后/异常时）。
 * 子类可覆写 {@link #onSuccess} / {@link #onFailure} 来实现日志落库等操作。
 *
 * @param <C> 插件上下文类型
 * @param <R> 插件结果类型
 */
@Slf4j
public abstract class PluginExecutorBase<C, R> {

    public ExecutionOutcome<R> execute(Plugin<C, R> plugin, C context) {
        long start = System.currentTimeMillis();
        try {
            R result = plugin.execute(context);
            long costMs = System.currentTimeMillis() - start;
            ExecutionOutcome<R> outcome = ExecutionOutcome.success(plugin.getPluginId(), result, costMs);
            onSuccess(plugin, context, outcome);
            return outcome;
        } catch (Exception e) {
            long costMs = System.currentTimeMillis() - start;
            log.error("[PluginExecutor] 插件执行异常: {}", plugin.getPluginId(), e);
            ExecutionOutcome<R> outcome = ExecutionOutcome.failure(plugin.getPluginId(), e, costMs);
            onFailure(plugin, context, outcome);
            return outcome;
        }
    }

    /**
     * 插件执行成功后回调。子类可覆写来实现日志落库等。
     */
    protected void onSuccess(Plugin<C, R> plugin, C context, ExecutionOutcome<R> outcome) {
        // default no-op
    }

    /**
     * 插件执行失败后回调。子类可覆写来实现日志落库等。
     */
    protected void onFailure(Plugin<C, R> plugin, C context, ExecutionOutcome<R> outcome) {
        // default no-op
    }
}
