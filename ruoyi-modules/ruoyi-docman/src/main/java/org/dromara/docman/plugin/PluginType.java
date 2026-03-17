package org.dromara.docman.plugin;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum PluginType {
    EXCEL_FILL("excel_fill", "Excel填充"),
    DATA_EXTRACT("data_extract", "数据提取"),
    AI_GENERATE("ai_generate", "AI生成");

    private final String code;
    private final String label;
}
