package org.dromara.docman.domain.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 工作流事项完成规则。
 */
@Getter
@AllArgsConstructor
public enum DocWorkflowTaskCompletionRule {

    PROJECT_BASIC_INFO_PRESENT("project_basic_info_present", "项目基础信息已具备"),
    DRAWING_EXISTS("drawing_exists", "存在有效图纸"),
    VISA_EXISTS("visa_exists", "存在有效签证"),
    WORKLOAD_EXISTS("workload_exists", "存在工作量记录"),
    ESTIMATE_SNAPSHOT_EXISTS("estimate_snapshot_exists", "存在估算快照"),
    BALANCE_ADJUSTMENT_EXISTS("balance_adjustment_exists", "存在平料记录");

    private final String code;
    private final String label;

    public static DocWorkflowTaskCompletionRule of(String code) {
        return Arrays.stream(values())
            .filter(item -> item.code.equals(code))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("未知事项完成规则: " + code));
    }
}
