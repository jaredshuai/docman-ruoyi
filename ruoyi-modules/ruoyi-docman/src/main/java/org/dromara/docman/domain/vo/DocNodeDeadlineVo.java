package org.dromara.docman.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class DocNodeDeadlineVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long processInstanceId;
    private String nodeCode;
    private Long projectId;
    private Integer durationDays;
    private LocalDate deadline;
    private Integer reminderCount;
    private LocalDateTime lastRemindedAt;
    private String projectName;
    private String nodeName;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
