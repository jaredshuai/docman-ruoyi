package org.dromara.docman.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 工作流节点事项视图对象
 */
@Data
public class DocWorkflowNodeTaskVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

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
    private Date createTime;
    private Date updateTime;
}
