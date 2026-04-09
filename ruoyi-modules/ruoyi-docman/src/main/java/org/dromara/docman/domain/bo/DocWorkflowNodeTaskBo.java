package org.dromara.docman.domain.bo;

import lombok.Data;

/**
 * 工作流节点事项业务对象
 */
@Data
public class DocWorkflowNodeTaskBo {

    private Long id;
    private String taskCode;
    private String taskName;
    private String taskType;
    private Boolean requiredFlag;
    private Integer sortOrder;
    private String completionRule;
    private String pluginCodes;
    private String description;
    private String status;
}
