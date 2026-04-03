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
 * 项目工作量记录表 doc_project_add_record
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("doc_project_add_record")
public class DocProjectAddRecord extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id")
    private Long id;

    /** 项目 id */
    private Long projectId;

    /** 是否启用 */
    private Boolean enable;

    /** 预估价格 */
    private BigDecimal estimatedPrice;

    /** 备注 */
    private String remark;

    @TableLogic
    private String delFlag;
}
