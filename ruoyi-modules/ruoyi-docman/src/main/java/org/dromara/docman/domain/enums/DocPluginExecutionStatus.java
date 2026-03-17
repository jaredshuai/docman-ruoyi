package org.dromara.docman.domain.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum DocPluginExecutionStatus {

    SUCCESS("success", "成功"),
    FAILED("failed", "失败");

    private final String code;
    private final String label;

    public static DocPluginExecutionStatus of(String code) {
        return Arrays.stream(values())
            .filter(item -> item.code.equals(code))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("未知插件执行状态: " + code));
    }
}
