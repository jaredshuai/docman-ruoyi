package org.dromara.docman.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class DocPluginStatsVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String pluginId;
    private String pluginName;
    private Long totalCount;
    private Long successCount;
    private Long failCount;
    private Double avgCostMs;
}
