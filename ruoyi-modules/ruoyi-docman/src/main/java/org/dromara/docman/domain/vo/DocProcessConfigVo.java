package org.dromara.docman.domain.vo;

import com.alibaba.excel.annotation.ExcelProperty;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.common.excel.annotation.ExcelDictFormat;
import org.dromara.common.excel.convert.ExcelDictConvert;
import org.dromara.docman.domain.entity.DocProcessConfig;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

@Data
@AutoMapper(target = DocProcessConfig.class)
public class DocProcessConfigVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long projectId;
    private Long definitionId;
    private Long instanceId;

    @ExcelProperty(value = "配置状态", converter = ExcelDictConvert.class)
    @ExcelDictFormat(dictType = "doc_process_status")
    private String status;
    private Date createTime;
    private Date updateTime;
}
