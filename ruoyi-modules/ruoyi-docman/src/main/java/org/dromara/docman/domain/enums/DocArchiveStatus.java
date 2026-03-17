package org.dromara.docman.domain.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum DocArchiveStatus {

    REQUESTED("requested", "已申请"),
    GENERATING("generating", "归档中"),
    COMPLETED("completed", "已完成"),
    FAILED("failed", "失败");

    private final String code;
    private final String label;

    public static DocArchiveStatus of(String code) {
        return Arrays.stream(values())
            .filter(item -> item.code.equals(code))
            .findFirst()
            .orElse(COMPLETED);
    }
}
