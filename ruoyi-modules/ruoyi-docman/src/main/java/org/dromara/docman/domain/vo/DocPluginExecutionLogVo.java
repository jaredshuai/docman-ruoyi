package org.dromara.docman.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.docman.domain.entity.DocPluginExecutionLog;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

@Data
@AutoMapper(target = DocPluginExecutionLog.class)
public class DocPluginExecutionLogVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long projectId;
    private Long processInstanceId;
    private String nodeCode;
    private String pluginId;
    private String pluginName;
    private String status;
    private Long costMs;
    private Integer generatedFileCount;
    private String errorMessage;
    private String requestSnapshot;
    private String resultSnapshot;
    private Date createTime;
}
