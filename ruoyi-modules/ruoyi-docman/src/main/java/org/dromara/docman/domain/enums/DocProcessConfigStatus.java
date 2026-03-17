package org.dromara.docman.domain.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.dromara.common.core.statemachine.StateEnum;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum DocProcessConfigStatus implements StateEnum<DocProcessConfigStatus> {

    PENDING("pending", "待启动"),
    RUNNING("running", "运行中"),
    COMPLETED("completed", "已完成");

    private final String code;
    private final String label;

    public static DocProcessConfigStatus of(String code) {
        return Arrays.stream(values())
            .filter(item -> item.code.equals(code))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("未知流程状态: " + code));
    }
}
