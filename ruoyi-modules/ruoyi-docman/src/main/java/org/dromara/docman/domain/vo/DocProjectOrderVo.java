package org.dromara.docman.domain.vo;

import cn.idev.excel.annotation.ExcelProperty;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.common.translation.annotation.Translation;
import org.dromara.common.translation.constant.TransConstant;
import org.dromara.docman.domain.entity.DocProjectOrder;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 项目签证单视图对象
 */
@Data
@AutoMapper(target = DocProjectOrder.class)
public class DocProjectOrderVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    /** 项目ID */
    private Long projectId;

    @Translation(type = TransConstant.USER_ID_TO_NAME, mapper = "projectId:projectName")
    private String projectName;

    /** 事由 */
    @ExcelProperty(value = "事由")
    private String reason;

    /** 日期 */
    @ExcelProperty(value = "日期")
    private Date date;

    /** 金额 */
    @ExcelProperty(value = "金额")
    private BigDecimal amount;

    /** 备注 */
    private String remark;

    private Long createBy;
    private Date createTime;
    private Date updateTime;
}
