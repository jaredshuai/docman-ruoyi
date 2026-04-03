package org.dromara.docman.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.dromara.docman.domain.entity.DocProjectAddRecord;

import java.math.BigDecimal;
import java.util.List;

/**
 * 项目工作量记录业务对象
 */
@Data
@AutoMapper(target = DocProjectAddRecord.class, reverseConvertGenerate = false)
public class DocProjectAddRecordBo {

    private Long id;

    @NotNull(message = "项目 ID 不能为空")
    private Long projectId;

    private Boolean enable;

    @DecimalMin(value = "0", message = "预估价格必须大于等于 0")
    private BigDecimal estimatedPrice;

    private String remark;

    /** 工作量明细则列表 */
    private List<DocProjectAddRecordDetailBo> details;
}
