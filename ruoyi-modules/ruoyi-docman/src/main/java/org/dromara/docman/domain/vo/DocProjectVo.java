package org.dromara.docman.domain.vo;

import cn.idev.excel.annotation.ExcelProperty;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.common.excel.annotation.ExcelDictFormat;
import org.dromara.common.excel.convert.ExcelDictConvert;
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

    /** 电信编号 */
    private String dianxinCode;

    /** 翔云编号 */
    private String xiangyunCode;

    /** 项目金额 */
    private java.math.BigDecimal price;

    @ExcelProperty(value = "客户类型", converter = ExcelDictConvert.class)
    @ExcelDictFormat(dictType = "doc_customer_type")
    private String customerType;

    @ExcelProperty(value = "业务类型", converter = ExcelDictConvert.class)
    @ExcelDictFormat(dictType = "doc_business_type")
    private String businessType;
    private String documentCategory;

    @ExcelProperty(value = "项目状态", converter = ExcelDictConvert.class)
    @ExcelDictFormat(dictType = "doc_project_status")
    private String status;
    
    /** 客户名称 */
    private String customerName;
    
    private Long ownerId;

    @Translation(type = TransConstant.USER_ID_TO_NAME, mapper = "ownerId")
    private String ownerName;
    
    /** 电信立项时间 */
    private Date dianxinInitiationTime;
    
    /** 计划开工时间 */
    private Date startTime;
    
    /** 计划完工时间 */
    private Date endTime;

    private String nasBasePath;
    private String nasDirStatus;
    private String remark;
    private List<Long> memberIds;
    private String currentUserRole;
    private Long createBy;
    private Date createTime;
    private Date updateTime;
}
