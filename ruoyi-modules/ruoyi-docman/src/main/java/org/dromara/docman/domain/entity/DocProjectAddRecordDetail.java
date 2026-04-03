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
 * 项目工作量记录详情表 doc_project_add_record_detail
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("doc_project_add_record_detail")
public class DocProjectAddRecordDetail extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id")
    private Long id;

    /** 项目 id */
    private Long projectId;

    /** 增加工作量记录 id */
    private Long projectAddRecordId;

    /** 工作量名称 */
    private String name;

    /** 别名 */
    private String alias;

    /** 价格 */
    private BigDecimal price;

    /** 备注 */
    private String remark;

    @TableLogic
    private String delFlag;
}
