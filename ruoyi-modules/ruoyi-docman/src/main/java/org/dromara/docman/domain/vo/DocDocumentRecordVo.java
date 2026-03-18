package org.dromara.docman.domain.vo;

import cn.idev.excel.annotation.ExcelProperty;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.common.excel.annotation.ExcelDictFormat;
import org.dromara.common.excel.convert.ExcelDictConvert;
import org.dromara.docman.domain.entity.DocDocumentRecord;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 文档记录视图对象
 */
@Data
@AutoMapper(target = DocDocumentRecord.class)
public class DocDocumentRecordVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long projectId;
    private String projectName;
    private Long nodeInstanceId;
    private String pluginId;

    @ExcelProperty(value = "来源类型", converter = ExcelDictConvert.class)
    @ExcelDictFormat(dictType = "doc_source_type")
    private String sourceType;
    private String fileName;
    private String nasPath;
    private Long ossId;

    @ExcelProperty(value = "文档状态", converter = ExcelDictConvert.class)
    @ExcelDictFormat(dictType = "doc_document_status")
    private String status;
    private Date generatedAt;
    private Date archivedAt;
    private Date createTime;
}
