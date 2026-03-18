package org.dromara.docman.domain.bo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.mybatis.core.domain.BaseEntity;

@Data
@EqualsAndHashCode(callSuper = true)
public class DocProjectMemberBo extends BaseEntity {

    private Long id;

    private Long projectId;

    @NotNull(message = "用户ID不能为空")
    private Long userId;

    @NotBlank(message = "角色类型不能为空")
    private String roleType;
}
