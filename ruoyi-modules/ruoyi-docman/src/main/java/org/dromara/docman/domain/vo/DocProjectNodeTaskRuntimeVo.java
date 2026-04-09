package org.dromara.docman.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 节点事项运行时视图对象
 */
@Data
public class DocProjectNodeTaskRuntimeVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long projectId;
    private String nodeCode;
    private String taskCode;
    private String taskName;
    private String taskType;
    private Boolean requiredFlag;
    private Integer sortOrder;
    private String completionRule;
    private String pluginCodes;
    private String status;
    private Long completedBy;
    private Date completedAt;
    private String evidenceRef;
}
