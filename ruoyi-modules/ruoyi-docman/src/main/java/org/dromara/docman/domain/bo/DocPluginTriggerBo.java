package org.dromara.docman.domain.bo;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DocPluginTriggerBo {

    @NotNull(message = "流程实例ID不能为空")
    private Long processInstanceId;

    /**
     * 可选。不传则触发该流程实例所有节点插件。
     */
    private String nodeCode;
}
