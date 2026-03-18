package org.dromara.docman.domain.vo;

import com.alibaba.excel.annotation.ExcelProperty;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.common.excel.annotation.ExcelDictFormat;
import org.dromara.common.excel.convert.ExcelDictConvert;
import org.dromara.common.translation.annotation.Translation;
import org.dromara.common.translation.constant.TransConstant;
import org.dromara.docman.domain.entity.DocProjectMember;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

@Data
@AutoMapper(target = DocProjectMember.class)
public class DocProjectMemberVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long projectId;
    private Long userId;

    @Translation(type = TransConstant.USER_ID_TO_NAME, mapper = "userId")
    private String userName;

    @ExcelProperty(value = "角色类型", converter = ExcelDictConvert.class)
    @ExcelDictFormat(dictType = "doc_member_role")
    private String roleType;
    private Date createTime;
}
