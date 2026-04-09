package org.dromara.docman.domain.bo;

import lombok.Data;

import java.util.List;

/**
 * 工作流模板节点业务对象
 */
@Data
public class DocWorkflowTemplateNodeBo {

    private Long id;
    private String nodeCode;
    private String nodeName;
    private Integer sortOrder;
    private String description;
    private String status;
    private List<DocWorkflowNodeTaskBo> tasks;
}
