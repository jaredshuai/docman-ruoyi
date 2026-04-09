package org.dromara.docman.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 项目图纸录入视图对象
 */
@Data
public class DocProjectDrawingVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long projectId;
    private String drawingCode;
    private String orderSerialNo;
    private String workContent;
    private Boolean includeInProject;
    private String remark;
    private Date createTime;
    private Date updateTime;
}
