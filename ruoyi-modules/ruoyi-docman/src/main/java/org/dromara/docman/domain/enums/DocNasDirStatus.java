package org.dromara.docman.domain.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum DocNasDirStatus {

    PENDING("pending", "待创建"),
    CREATED("created", "已创建"),
    FAILED("failed", "创建失败");

    private final String code;
    private final String label;

    public static DocNasDirStatus of(String code) {
        return Arrays.stream(values())
            .filter(item -> item.code.equals(code))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("未知NAS目录状态: " + code));
    }
}
