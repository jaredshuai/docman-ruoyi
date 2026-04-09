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
 * 项目平料与价格调整表 doc_project_balance_adjustment
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("doc_project_balance_adjustment")
public class DocProjectBalanceAdjustment extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id")
    private Long id;

    private Long projectId;

    private BigDecimal materialPrice;

    private String balanceRemark;

    private String status;

    @TableLogic
    private String delFlag;
}
