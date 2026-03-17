package org.dromara.docman.domain.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum DocProjectRole {

    OWNER("owner", "项目负责人"),
    EDITOR("editor", "项目编辑"),
    VIEWER("viewer", "项目只读");

    private final String code;
    private final String label;

    public static DocProjectRole of(String code) {
        return Arrays.stream(values())
            .filter(item -> item.code.equals(code))
            .findFirst()
            .orElse(VIEWER);
    }
}
