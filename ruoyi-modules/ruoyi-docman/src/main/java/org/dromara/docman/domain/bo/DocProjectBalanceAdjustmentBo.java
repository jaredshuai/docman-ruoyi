package org.dromara.docman.domain.bo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 平料调整业务对象
 */
@Data
public class DocProjectBalanceAdjustmentBo {

    private Long id;
    private Long projectId;
    private BigDecimal materialPrice;
    private String balanceRemark;
    private String status;
}
