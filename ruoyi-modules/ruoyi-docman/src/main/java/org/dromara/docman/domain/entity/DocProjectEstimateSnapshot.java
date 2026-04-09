package org.dromara.docman.domain.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.mybatis.core.domain.BaseEntity;

import java.io.Serial;
import java.math.BigDecimal;

/**
 * 项目估算结果快照表 doc_project_estimate_snapshot
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("doc_project_estimate_snapshot")
public class DocProjectEstimateSnapshot extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id")
    private Long id;

    private Long projectId;

    private String estimateType;

    private BigDecimal estimateAmount;

    private Long drawingCount;

    private Long visaCount;

    private String status;

    private String summary;

    @TableLogic
    private String delFlag;
}
