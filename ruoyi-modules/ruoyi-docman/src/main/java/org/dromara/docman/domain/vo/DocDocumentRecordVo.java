package org.dromara.docman.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
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
    private String sourceType;
    private String fileName;
    private String nasPath;
    private Long ossId;
    private String status;
    private Date generatedAt;
    private Date archivedAt;
    private Date createTime;
}
