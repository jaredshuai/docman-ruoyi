package org.dromara.docman.domain.bo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 电信工作量项业务对象
 */
@Data
public class DocTelecomWorkloadItemBo {

    private Long id;
    private String itemCode;
    private String itemName;
    private String category;
    private String unit;
    private BigDecimal defaultPrice;
    private String description;
    private Integer sortOrder;
    private String status;
}
