package org.dromara.docman.domain.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.mybatis.core.domain.BaseEntity;

import java.io.Serial;

/**
 * 工作流模板节点表 doc_workflow_template_node
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("doc_workflow_template_node")
public class DocWorkflowTemplateNode extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id")
    private Long id;

    private Long templateId;

    private String nodeCode;

    private String nodeName;

    private Integer sortOrder;

    private String description;

    private String status;

    @TableLogic
    private String delFlag;
}
