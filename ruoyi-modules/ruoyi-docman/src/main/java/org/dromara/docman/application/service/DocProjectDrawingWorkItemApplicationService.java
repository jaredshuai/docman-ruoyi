package org.dromara.docman.application.service;

import lombok.RequiredArgsConstructor;
import org.dromara.common.core.application.CommandApplicationService;
import org.dromara.docman.domain.bo.DocProjectDrawingWorkItemBo;
import org.dromara.docman.service.IDocProjectDrawingWorkItemService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DocProjectDrawingWorkItemApplicationService implements CommandApplicationService {

    private final IDocProjectDrawingWorkItemService drawingWorkItemService;

    /**
     * 保存图纸工作量映射。
     *
     * @param bo 图纸工作量映射参数
     * @return 映射ID
     */
    public Long save(DocProjectDrawingWorkItemBo bo) {
        return drawingWorkItemService.save(bo);
    }

    /**
     * 批量删除图纸工作量映射。
     *
     * @param ids 映射ID列表
     */
    public void delete(List<Long> ids) {
        drawingWorkItemService.deleteByIds(ids);
    }
}
