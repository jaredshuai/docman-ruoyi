package org.dromara.docman.domain.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 项目节点事项运行时状态。
 */
@Getter
@AllArgsConstructor
public enum DocProjectNodeTaskStatus {

    PENDING("pending", "待完成"),
    COMPLETED("completed", "已完成"),
    SKIPPED("skipped", "已跳过");

    private final String code;
    private final String label;

    public static DocProjectNodeTaskStatus of(String code) {
        return Arrays.stream(values())
            .filter(item -> item.code.equals(code))
            .findFirst()
            .orElse(PENDING);
    }

    public boolean isDone() {
        return this == COMPLETED || this == SKIPPED;
    }
}
