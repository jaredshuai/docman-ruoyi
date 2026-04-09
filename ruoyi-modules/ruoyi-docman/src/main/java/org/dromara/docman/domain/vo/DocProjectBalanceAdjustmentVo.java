package org.dromara.docman.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 平料调整视图对象
 */
@Data
public class DocProjectBalanceAdjustmentVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long projectId;
    private BigDecimal materialPrice;
    private String balanceRemark;
    private String status;
    private Date createTime;
    private Date updateTime;
}
