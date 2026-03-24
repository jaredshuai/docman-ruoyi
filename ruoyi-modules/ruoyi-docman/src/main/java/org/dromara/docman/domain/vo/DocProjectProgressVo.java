package org.dromara.docman.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class DocProjectProgressVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long projectId;
    private String projectName;
    private Long totalNodes;
    private Long completedNodes;
    private Long totalDocuments;
    private Long pendingDocuments;
    private Double progressRate;
}
