package org.dromara.docman.domain.bo;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 项目签证录入业务对象
 */
@Data
public class DocProjectVisaBo {

    private Long id;

    @NotNull(message = "项目ID不能为空")
    private Long projectId;
    private String reason;
    private String contentBasis;
    private BigDecimal amount;
    private Date visaDate;
    private Boolean includeInProject;
    private String remark;
}
