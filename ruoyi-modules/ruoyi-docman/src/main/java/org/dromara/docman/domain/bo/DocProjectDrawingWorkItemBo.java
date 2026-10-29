package org.dromara.docman.domain.bo;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
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

    @NotBlank(message = "工作量名称不能为空")
    private String workItemName;
    private BigDecimal technician;
    private BigDecimal technicianCoefficient;
    private BigDecimal generalWorker;
    private BigDecimal generalWorkerCoefficient;
    private BigDecimal machineShift;
    private BigDecimal machineShiftUnitPrice;
    private BigDecimal machineShiftCoefficient;
    private BigDecimal instrumentShift;
    private BigDecimal instrumentShiftUnitPrice;
    private BigDecimal instrumentShiftCoefficient;
    private BigDecimal materialQuantity;
    private BigDecimal materialUnitPrice;
}
