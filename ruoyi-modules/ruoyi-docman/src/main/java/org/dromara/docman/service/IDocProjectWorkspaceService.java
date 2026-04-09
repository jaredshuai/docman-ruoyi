package org.dromara.docman.service;

import org.dromara.docman.domain.bo.DocProjectAdvanceNodeBo;
import org.dromara.docman.domain.bo.DocProjectNodeTaskCompleteBo;
import org.dromara.docman.domain.vo.DocProjectWorkspaceVo;

public interface IDocProjectWorkspaceService {

    DocProjectWorkspaceVo getWorkspace(Long projectId);

    void completeTask(Long projectId, Long taskRuntimeId, DocProjectNodeTaskCompleteBo bo);

    void triggerTaskPlugins(Long projectId, Long taskRuntimeId);

    void advanceNode(DocProjectAdvanceNodeBo bo);

    void triggerEstimate(Long projectId);

    void triggerExportText(Long projectId);
}
