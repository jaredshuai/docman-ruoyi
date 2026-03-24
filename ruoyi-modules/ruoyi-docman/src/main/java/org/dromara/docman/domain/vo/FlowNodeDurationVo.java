package org.dromara.docman.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class FlowNodeDurationVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long nodeId;
    private String nodeCode;
    private String nodeName;
    private Integer durationDays;
}
