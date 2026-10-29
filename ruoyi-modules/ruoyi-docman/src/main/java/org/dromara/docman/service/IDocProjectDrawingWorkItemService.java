package org.dromara.docman.service;

import org.dromara.docman.domain.bo.DocProjectDrawingWorkItemBo;
import org.dromara.docman.domain.vo.DocProjectDrawingWorkItemVo;

import java.util.List;

public interface IDocProjectDrawingWorkItemService {

    List<DocProjectDrawingWorkItemVo> listByProject(Long projectId);

    List<DocProjectDrawingWorkItemVo> listByDrawing(Long projectId, Long drawingId);

    Long save(DocProjectDrawingWorkItemBo bo);

    void deleteByIds(List<Long> ids);
}
