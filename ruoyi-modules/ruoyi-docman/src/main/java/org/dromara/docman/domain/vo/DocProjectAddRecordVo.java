package org.dromara.docman.domain.vo;

import cn.idev.excel.annotation.ExcelProperty;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.common.excel.annotation.ExcelDictFormat;
import org.dromara.common.excel.convert.ExcelDictConvert;
import org.dromara.common.translation.annotation.Translation;
import org.dromara.common.translation.constant.TransConstant;
import org.dromara.docman.domain.entity.DocProjectAddRecord;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * 项目工作量记录视图对象
 */
@Data
@AutoMapper(target = DocProjectAddRecord.class)
public class DocProjectAddRecordVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long projectId;

    @ExcelProperty(value = "是否启用", converter = ExcelDictConvert.class)
    @ExcelDictFormat(dictType = "sys_yes_no")
    private Boolean enable;

    @ExcelProperty(value = "预估价格")
    private BigDecimal estimatedPrice;

    private String remark;

    /** 工作量明细则列表 */
    private List<DocProjectAddRecordDetailVo> details;

    private Long createBy;
    private Date createTime;
    private Date updateTime;
}
