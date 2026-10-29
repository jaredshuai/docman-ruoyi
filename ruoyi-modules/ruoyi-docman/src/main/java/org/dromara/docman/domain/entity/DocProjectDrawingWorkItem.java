package org.dromara.docman.domain.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.mybatis.core.domain.BaseEntity;

import java.io.Serial;
import java.math.BigDecimal;

/**
 * 项目图纸工作量项映射表 doc_project_drawing_work_item
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("doc_project_drawing_work_item")
public class DocProjectDrawingWorkItem extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id")
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

    @TableLogic
    private String delFlag;
}
