package org.dromara.docman.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.mybatis.core.domain.BaseEntity;
import org.dromara.docman.domain.entity.DocProject;

import java.util.List;

/**
 * 项目业务对象
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = DocProject.class, reverseConvertGenerate = false)
public class DocProjectBo extends BaseEntity {

    private Long id;

    @NotBlank(message = "项目名称不能为空")
    private String name;

    @NotBlank(message = "客户类型不能为空")
    private String customerType;

    @NotBlank(message = "业务类型不能为空")
    private String businessType;

    @NotBlank(message = "文档类别不能为空")
    private String documentCategory;

    @NotNull(message = "负责人不能为空")
    private Long ownerId;

    private String remark;

    /** 项目成员ID列表 */
    private List<Long> memberIds;
}
