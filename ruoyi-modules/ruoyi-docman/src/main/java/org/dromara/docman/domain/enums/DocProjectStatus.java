package org.dromara.docman.domain.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum DocProjectStatus {

    ACTIVE("active", "进行中"),
    ARCHIVED("archived", "已归档");

    private final String code;
    private final String label;

    public static DocProjectStatus of(String code) {
        return Arrays.stream(values())
            .filter(item -> item.code.equals(code))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("未知项目状态: " + code));
    }
}
