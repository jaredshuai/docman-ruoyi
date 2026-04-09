package org.dromara.docman.domain.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.dromara.common.core.statemachine.StateEnum;

import java.util.Arrays;

/**
 * 电信项目流程运行时状态。
 */
@Getter
@AllArgsConstructor
public enum DocProjectRuntimeStatus implements StateEnum<DocProjectRuntimeStatus> {

    RUNNING("running", "运行中"),
    COMPLETED("completed", "已完成");

    private final String code;
    private final String label;

    public static DocProjectRuntimeStatus of(String code) {
        return Arrays.stream(values())
            .filter(item -> item.code.equals(code))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("未知项目运行时状态: " + code));
    }
}
