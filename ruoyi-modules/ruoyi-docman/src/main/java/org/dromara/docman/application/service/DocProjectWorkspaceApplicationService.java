package org.dromara.docman.application.service;

import lombok.RequiredArgsConstructor;
import org.dromara.common.core.application.CommandApplicationService;
import org.dromara.common.core.application.QueryApplicationService;
import org.dromara.docman.domain.bo.DocProjectAdvanceNodeBo;
import org.dromara.docman.domain.bo.DocProjectNodeTaskCompleteBo;
import org.dromara.docman.domain.vo.DocProjectWorkspaceVo;
import org.dromara.docman.service.IDocProjectWorkspaceService;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DocProjectWorkspaceApplicationService implements CommandApplicationService, QueryApplicationService {

    private final IDocProjectWorkspaceService workspaceService;

    public DocProjectWorkspaceVo getWorkspace(Long projectId) {
        return workspaceService.getWorkspace(projectId);
    }

    public void completeTask(Long projectId, Long taskRuntimeId, DocProjectNodeTaskCompleteBo bo) {
        workspaceService.completeTask(projectId, taskRuntimeId, bo);
    }

    public void triggerTaskPlugins(Long projectId, Long taskRuntimeId) {
        workspaceService.triggerTaskPlugins(projectId, taskRuntimeId);
    }

    public void advanceNode(DocProjectAdvanceNodeBo bo) {
        workspaceService.advanceNode(bo);
    }
}
