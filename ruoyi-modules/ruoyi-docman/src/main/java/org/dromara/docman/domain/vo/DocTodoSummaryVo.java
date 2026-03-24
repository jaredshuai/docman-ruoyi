package org.dromara.docman.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class DocTodoSummaryVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long myProjectCount;
    private Long activeProjectCount;
    private Long overdueNodeCount;
    private Long waitingTaskCount;
    private Long copiedTaskCount;
    private Long finishedTaskCount;
}
