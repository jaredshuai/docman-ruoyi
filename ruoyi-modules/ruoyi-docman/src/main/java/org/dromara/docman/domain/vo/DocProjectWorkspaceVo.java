package org.dromara.docman.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 项目工作台视图对象
 */
@Data
public class DocProjectWorkspaceVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long projectId;
    private String projectName;
    private String projectTypeCode;
    private String currentNodeCode;
    private String currentNodeName;
    private String runtimeStatus;
    private List<DocWorkflowTemplateNodeVo> nodes;
    private List<DocProjectNodeTaskRuntimeVo> currentNodeTasks;
    private Long drawingCount;
    private Long includedDrawingCount;
    private Long visaCount;
    private Long includedVisaCount;
    private Boolean estimateTriggerReady;
    private String estimateTriggerBlockedReason;
    private Boolean exportTriggerReady;
    private String exportTriggerBlockedReason;
    private DocProjectEstimateSnapshotVo latestEstimateSnapshot;
    private DocDocumentRecordVo latestExportArtifact;
}
