package org.dromara.docman.domain.bo;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 图纸工作量映射业务对象
 */
@Data
public class DocProjectDrawingWorkItemBo {

    private Long id;

    @NotNull(message = "项目ID不能为空")
    private Long projectId;

    @NotNull(message = "图纸ID不能为空")
    private Long drawingId;
    private String workItemCode;
    private String workItemName;
    private String category;
    private String unit;
    private BigDecimal quantity;
    private Boolean includeInEstimate;
    private String remark;
}
