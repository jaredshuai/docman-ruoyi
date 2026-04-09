package org.dromara.docman.application.service;

import lombok.RequiredArgsConstructor;
import org.dromara.common.core.application.CommandApplicationService;
import org.dromara.docman.service.IDocProjectWorkspaceService;
import org.springframework.stereotype.Service;

/**
 * 项目估算命令编排服务。
 */
@Service
@RequiredArgsConstructor
public class DocProjectEstimateApplicationService implements CommandApplicationService {

    private final IDocProjectWorkspaceService workspaceService;

    /**
     * 手动触发项目当前节点的初步估算。
     *
     * @param projectId 项目ID
     */
    public void triggerEstimate(Long projectId) {
        workspaceService.triggerEstimate(projectId);
    }
}
