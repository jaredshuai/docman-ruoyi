package org.dromara.docman.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.mybatis.core.domain.BaseEntity;
import org.dromara.docman.domain.entity.DocDocumentRecord;

/**
 * 文档记录业务对象
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = DocDocumentRecord.class, reverseConvertGenerate = false)
public class DocDocumentRecordBo extends BaseEntity {

    private Long id;

    @NotNull(message = "项目ID不能为空")
    private Long projectId;

    private Long nodeInstanceId;

    @NotBlank(message = "来源类型不能为空")
    private String sourceType;

    @NotBlank(message = "文件名不能为空")
    private String fileName;

    /** 手动上传时由前端传入 */
    private String nasPath;

    /** 关联 sys_oss 表ID */
    private Long ossId;
}
