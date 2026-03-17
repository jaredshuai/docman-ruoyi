package org.dromara.docman.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.common.translation.annotation.Translation;
import org.dromara.common.translation.constant.TransConstant;
import org.dromara.docman.domain.entity.DocProject;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 项目视图对象
 */
@Data
@AutoMapper(target = DocProject.class)
public class DocProjectVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private String name;
    private String customerType;
    private String businessType;
    private String documentCategory;
    private String status;
    private Long ownerId;

    @Translation(type = TransConstant.USER_ID_TO_NAME, mapper = "ownerId")
    private String ownerName;

    private String nasBasePath;
    private String nasDirStatus;
    private String remark;
    private List<Long> memberIds;
    private String currentUserRole;
    private Long createBy;
    private Date createTime;
    private Date updateTime;
}
