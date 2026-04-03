package org.dromara.docman.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.dromara.docman.domain.entity.DocProjectAddRecordDetail;

import java.math.BigDecimal;

/**
 * 项目工作量记录详情业务对象
 */
@Data
@AutoMapper(target = DocProjectAddRecordDetail.class, reverseConvertGenerate = false)
public class DocProjectAddRecordDetailBo {

    private Long id;

    @NotNull(message = "项目 ID 不能为空")
    private Long projectId;

    @NotNull(message = "工作量记录 ID 不能为空")
    private Long projectAddRecordId;

    private String name;

    private String alias;

    @DecimalMin(value = "0", message = "价格必须大于等于 0")
    private BigDecimal price;

    private String remark;
}
