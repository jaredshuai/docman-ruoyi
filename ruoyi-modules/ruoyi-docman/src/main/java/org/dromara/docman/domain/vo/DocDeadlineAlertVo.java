package org.dromara.docman.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;

@Data
public class DocDeadlineAlertVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long projectId;
    private String projectName;
    private String nodeCode;
    private String nodeName;
    private LocalDate deadline;
    private Integer remainDays;
    private Integer reminderCount;
}
