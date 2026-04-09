package org.dromara.docman.domain.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.mybatis.core.domain.BaseEntity;

import java.io.Serial;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 项目签证单表 doc_project_order
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("doc_project_order")
public class DocProjectOrder extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id")
    private Long id;

    /** 项目ID */
    private Long projectId;

    /** 事由 */
    private String reason;

    /** 日期 */
    private Date date;

    /** 金额 */
    private BigDecimal amount;

    /** 备注 */
    private String remark;

    @TableLogic
    private String delFlag;
}
