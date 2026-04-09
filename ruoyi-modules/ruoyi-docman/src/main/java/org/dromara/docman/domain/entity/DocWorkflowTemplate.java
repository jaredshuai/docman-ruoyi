package org.dromara.docman.domain.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.mybatis.core.domain.BaseEntity;

import java.io.Serial;

/**
 * 工作流模板定义表 doc_workflow_template
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("doc_workflow_template")
public class DocWorkflowTemplate extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id")
    private Long id;

    private String code;

    private String name;

    private String projectTypeCode;

    private String description;

    private Boolean defaultFlag;

    private Integer sortOrder;

    private String status;

    @TableLogic
    private String delFlag;
}
