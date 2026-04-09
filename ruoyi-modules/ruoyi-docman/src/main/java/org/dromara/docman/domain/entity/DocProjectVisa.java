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
 * 项目签证录入表 doc_project_visa
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("doc_project_visa")
public class DocProjectVisa extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id")
    private Long id;

    private Long projectId;

    private String reason;

    private String contentBasis;

    private BigDecimal amount;

    private Date visaDate;

    private Boolean includeInProject;

    private String remark;

    @TableLogic
    private String delFlag;
}
