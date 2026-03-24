package org.dromara.docman.domain.bo;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class NodeDurationBo {
    @NotNull(message = "节点ID不能为空")
    private Long nodeId;

    @NotNull(message = "期限天数不能为空")
    @Min(value = 0, message = "期限天数不能为负数")
    private Integer durationDays;
}
