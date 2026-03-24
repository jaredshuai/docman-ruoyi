package org.dromara.docman.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class DocDashboardOverviewVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long totalProjects;
    private Long activeProjects;
    private Long totalDocuments;
    private Long pendingDocuments;
    private Long overdueNodes;
    private Long pluginFailCount;
}
