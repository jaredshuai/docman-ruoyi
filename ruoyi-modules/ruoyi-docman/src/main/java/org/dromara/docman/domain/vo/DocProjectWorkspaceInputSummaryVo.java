package org.dromara.docman.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 工作台输入数据状态摘要
 */
@Data
public class DocProjectWorkspaceInputSummaryVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long totalCount;
    private Long includedCount;
    private Boolean ready;
    private Date lastUpdatedTime;
}
