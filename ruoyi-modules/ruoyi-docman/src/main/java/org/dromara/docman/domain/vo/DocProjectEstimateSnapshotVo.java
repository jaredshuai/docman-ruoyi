package org.dromara.docman.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 项目估算快照视图对象
 */
@Data
public class DocProjectEstimateSnapshotVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long projectId;
    private String estimateType;
    private BigDecimal estimateAmount;
    private Long drawingCount;
    private Long visaCount;
    private String status;
    private String summary;
    private Date createTime;
}
