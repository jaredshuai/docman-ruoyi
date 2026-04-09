package org.dromara.docman.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 图纸工作量映射视图对象
 */
@Data
public class DocProjectDrawingWorkItemVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long projectId;
    private Long drawingId;
    private String workItemCode;
    private String workItemName;
    private String category;
    private String unit;
    private BigDecimal quantity;
    private Boolean includeInEstimate;
    private String remark;
    private Date createTime;
    private Date updateTime;
}
