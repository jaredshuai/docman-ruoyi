package org.dromara.docman.domain.bo;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 项目图纸录入业务对象
 */
@Data
public class DocProjectDrawingBo {

    private Long id;

    @NotNull(message = "项目ID不能为空")
    private Long projectId;
    private String drawingCode;
    private String orderSerialNo;
    private String workContent;
    private Boolean includeInProject;
    private String remark;
}
