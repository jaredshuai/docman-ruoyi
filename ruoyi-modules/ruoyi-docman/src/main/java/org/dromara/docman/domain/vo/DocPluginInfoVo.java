package org.dromara.docman.domain.vo;

import lombok.Data;
import org.dromara.docman.plugin.FieldDef;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Data
public class DocPluginInfoVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String pluginId;
    private String pluginName;
    private String pluginType;
    private List<FieldDef> inputFields;
    private List<FieldDef> outputFields;
}
