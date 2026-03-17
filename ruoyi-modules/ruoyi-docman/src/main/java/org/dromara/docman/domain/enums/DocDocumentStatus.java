package org.dromara.docman.domain.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.dromara.common.core.statemachine.StateEnum;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum DocDocumentStatus implements StateEnum<DocDocumentStatus> {

    PENDING("pending", "待生成"),
    RUNNING("running", "生成中"),
    GENERATED("generated", "已生成"),
    FAILED("failed", "生成失败"),
    ARCHIVED("archived", "已归档"),
    OBSOLETE("obsolete", "已失效");

    private final String code;
    private final String label;

    public static DocDocumentStatus of(String code) {
        return Arrays.stream(values())
            .filter(item -> item.code.equals(code))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("未知文档状态: " + code));
    }
}
