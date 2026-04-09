package org.dromara.docman.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 工作流模板视图对象
 */
@Data
public class DocWorkflowTemplateVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private String code;
    private String name;
    private String projectTypeCode;
    private String description;
    private Boolean defaultFlag;
    private Integer sortOrder;
    private String status;
    private List<DocWorkflowTemplateNodeVo> nodes;
    private Date createTime;
    private Date updateTime;
}
