package org.dromara.docman.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 电信工作量项视图对象
 */
@Data
public class DocTelecomWorkloadItemVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private String itemCode;
    private String itemName;
    private String category;
    private String unit;
    private BigDecimal defaultPrice;
    private String description;
    private Integer sortOrder;
    private String status;
    private Date createTime;
    private Date updateTime;
}
