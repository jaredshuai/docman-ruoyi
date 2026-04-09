package org.dromara.docman.domain.bo;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 项目类型业务对象
 */
@Data
public class DocProjectTypeBo {

    private Long id;

    @NotBlank(message = "项目类型编码不能为空")
    private String code;

    @NotBlank(message = "项目类型名称不能为空")
    private String name;

    @NotBlank(message = "客户类型不能为空")
    private String customerType;

    private String description;

    private Integer sortOrder;

    private String status;
}
