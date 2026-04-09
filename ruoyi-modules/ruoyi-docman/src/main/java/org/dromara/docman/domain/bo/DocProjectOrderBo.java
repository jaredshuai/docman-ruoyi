package org.dromara.docman.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.dromara.docman.domain.entity.DocProjectOrder;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 项目签证单业务对象
 */
@Data
@AutoMapper(target = DocProjectOrder.class, reverseConvertGenerate = false)
public class DocProjectOrderBo {

    private Long id;

    @NotNull(message = "项目ID不能为空")
    private Long projectId;

    /** 事由 */
    private String reason;

    /** 日期 */
    private Date date;

    /** 金额 */
    @DecimalMin(value = "0", message = "金额必须大于等于0")
    private BigDecimal amount;

    /** 备注 */
    private String remark;
}
