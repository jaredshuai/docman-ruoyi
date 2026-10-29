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
 * 电信工作量项字典表 doc_telecom_workload_item
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("doc_telecom_workload_item")
public class DocTelecomWorkloadItem extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id")
    private Long id;

    private String itemCode;

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

    @TableLogic
    private String delFlag;
}
