package org.dromara.docman.domain.bo;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 电信工作量项业务对象
 */
@Data
public class DocTelecomWorkloadItemBo {

    private Long id;
    private String itemCode;
    @NotBlank(message = "工作量名称不能为空")
    private String itemName;
    private String category;
    private String unit;
    private BigDecimal defaultPrice;
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
    private String description;
    private Integer sortOrder;
    private String status;
}
