package org.dromara.docman.domain.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum DocDocumentSourceType {

    PLUGIN("plugin", "插件自动生成"),
    UPLOAD("upload", "手动上传"),
    ARCHIVE_MANIFEST("archive_manifest", "归档清单");

    private final String code;
    private final String label;

    public static DocDocumentSourceType of(String code) {
        return Arrays.stream(values())
            .filter(item -> item.code.equals(code))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("未知文档来源类型: " + code));
    }
}
