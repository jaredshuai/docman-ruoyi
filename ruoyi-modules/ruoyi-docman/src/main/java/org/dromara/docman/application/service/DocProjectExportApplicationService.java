package org.dromara.docman.application.service;

import lombok.RequiredArgsConstructor;
import org.dromara.common.core.application.CommandApplicationService;
import org.dromara.docman.service.IDocProjectWorkspaceService;
import org.springframework.stereotype.Service;

/**
 * 项目文本导出命令编排服务。
 */
@Service
@RequiredArgsConstructor
public class DocProjectExportApplicationService implements CommandApplicationService {

    private final IDocProjectWorkspaceService workspaceService;

    /**
     * 触发项目文本导出。
     *
     * @param projectId 项目ID
     */
    public void triggerExportText(Long projectId) {
        workspaceService.triggerExportText(projectId);
    }
}
