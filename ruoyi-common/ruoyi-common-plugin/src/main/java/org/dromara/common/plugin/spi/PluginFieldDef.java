package org.dromara.common.plugin.spi;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 通用插件字段定义。描述插件的输入/输出字段元数据。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PluginFieldDef implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String name;
    private String type;
    private boolean required;
    private String description;
}
