package org.dromara.docman.domain.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.mybatis.core.domain.BaseEntity;

import java.io.Serial;

/**
 * 工作流节点事项定义表 doc_workflow_node_task
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("doc_workflow_node_task")
public class DocWorkflowNodeTask extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id")
    private Long id;

    private Long nodeId;

    private String taskCode;

    private String taskName;

    private String taskType;

    private Boolean requiredFlag;

    private Integer sortOrder;

    private String completionRule;

    private String pluginCodes;

    private String description;

    private String status;

    @TableLogic
    private String delFlag;
}
