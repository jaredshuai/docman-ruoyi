package org.dromara.docman.domain.bo;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

/**
 * 工作流模板业务对象
 */
@Data
public class DocWorkflowTemplateBo {

    private Long id;

    @NotBlank(message = "模板编码不能为空")
    private String code;

    @NotBlank(message = "模板名称不能为空")
    private String name;

    @NotBlank(message = "项目类型编码不能为空")
    private String projectTypeCode;

    private String description;
    private Boolean defaultFlag;
    private Integer sortOrder;
    private String status;
    private List<DocWorkflowTemplateNodeBo> nodes;
}
