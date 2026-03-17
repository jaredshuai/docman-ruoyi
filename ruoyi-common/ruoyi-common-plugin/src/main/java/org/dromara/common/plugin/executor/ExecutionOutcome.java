package org.dromara.common.plugin.executor;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 通用插件执行结果包装。包含耗时、成功标记、原始结果或异常信息。
 *
 * @param <R> 插件原始结果类型
 */
@Data
public class ExecutionOutcome<R> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String pluginId;
    private boolean success;
    private R result;
    private long costMs;
    private String errorMessage;
    private transient Exception exception;

    public static <R> ExecutionOutcome<R> success(String pluginId, R result, long costMs) {
        ExecutionOutcome<R> outcome = new ExecutionOutcome<>();
        outcome.setPluginId(pluginId);
        outcome.setSuccess(true);
        outcome.setResult(result);
        outcome.setCostMs(costMs);
        return outcome;
    }

    public static <R> ExecutionOutcome<R> failure(String pluginId, Exception e, long costMs) {
        ExecutionOutcome<R> outcome = new ExecutionOutcome<>();
        outcome.setPluginId(pluginId);
        outcome.setSuccess(false);
        outcome.setCostMs(costMs);
        outcome.setErrorMessage(e.getMessage());
        outcome.setException(e);
        return outcome;
    }
}
