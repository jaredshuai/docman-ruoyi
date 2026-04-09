package org.dromara.docman.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.dromara.docman.domain.entity.DocProject;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * 项目业务对象
 */
@Data
@AutoMapper(target = DocProject.class, reverseConvertGenerate = false)
public class DocProjectBo {

    private Long id;

    @NotBlank(message = "项目名称不能为空")
    private String name;

    /** 电信编号 */
    private String dianxinCode;

    /** 翔云编号 */
    private String xiangyunCode;

    /** 项目金额 */
    private BigDecimal price;

    /** 项目类型编码 */
    private String projectTypeCode;

    @NotBlank(message = "客户类型不能为空")
    private String customerType;

    @NotBlank(message = "业务类型不能为空")
    private String businessType;

    @NotBlank(message = "文档类别不能为空")
    private String documentCategory;

    /** 客户名称 */
    private String customerName;

    @NotNull(message = "负责人不能为空")
    private Long ownerId;

    /** 电信立项时间 */
    private Date dianxinInitiationTime;

    /** 计划开工时间 */
    private Date startTime;

    /** 计划完工时间 */
    private Date endTime;

    private String remark;

    /** 项目成员ID列表 */
    private List<Long> memberIds;
}
