package org.dromara.docman.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 工作流模板节点视图对象
 */
@Data
public class DocWorkflowTemplateNodeVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long templateId;
    private String nodeCode;
    private String nodeName;
    private Integer sortOrder;
    private String description;
    private String status;
    private List<DocWorkflowNodeTaskVo> tasks;
    private Date createTime;
    private Date updateTime;
}
