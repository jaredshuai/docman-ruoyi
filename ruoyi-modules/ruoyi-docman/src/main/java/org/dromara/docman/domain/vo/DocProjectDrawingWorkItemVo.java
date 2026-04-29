package org.dromara.docman.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 图纸工作量映射视图对象
 */
@Data
public class DocProjectDrawingWorkItemVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long projectId;
    private Long drawingId;
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
    private Date createTime;
    private Date updateTime;
}
