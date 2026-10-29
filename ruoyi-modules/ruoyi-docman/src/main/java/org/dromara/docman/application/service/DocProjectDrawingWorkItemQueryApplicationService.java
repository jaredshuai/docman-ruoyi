package org.dromara.docman.application.service;

import lombok.RequiredArgsConstructor;
import org.dromara.common.core.application.QueryApplicationService;
import org.dromara.docman.domain.vo.DocProjectDrawingWorkItemVo;
import org.dromara.docman.service.IDocProjectDrawingWorkItemService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DocProjectDrawingWorkItemQueryApplicationService implements QueryApplicationService {

    private final IDocProjectDrawingWorkItemService drawingWorkItemService;

    /**
     * 查询项目下的全部图纸工作量映射列表。
     *
     * @param projectId 项目ID
     * @return 图纸工作量映射列表
     */
    public List<DocProjectDrawingWorkItemVo> listByProject(Long projectId) {
        return drawingWorkItemService.listByProject(projectId);
    }

    /**
     * 查询图纸下的工作量映射列表。
     *
     * @param projectId 项目ID
     * @param drawingId 图纸ID
     * @return 图纸工作量映射列表
     */
    public List<DocProjectDrawingWorkItemVo> listByDrawing(Long projectId, Long drawingId) {
        return drawingWorkItemService.listByDrawing(projectId, drawingId);
    }
}
