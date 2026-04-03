package org.dromara.docman.domain.vo;

import cn.idev.excel.annotation.ExcelProperty;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.common.excel.annotation.ExcelDictFormat;
import org.dromara.common.excel.convert.ExcelDictConvert;
import org.dromara.docman.domain.entity.DocProjectAddRecordDetail;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 项目工作量记录详情视图对象
 */
@Data
@AutoMapper(target = DocProjectAddRecordDetail.class)
public class DocProjectAddRecordDetailVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long projectId;
    private Long projectAddRecordId;

    @ExcelProperty(value = "工作量名称")
    private String name;

    @ExcelProperty(value = "别名")
    private String alias;

    @ExcelProperty(value = "价格")
    private BigDecimal price;

    private String remark;

    private Long createBy;
    private Date createTime;
    private Date updateTime;
}
