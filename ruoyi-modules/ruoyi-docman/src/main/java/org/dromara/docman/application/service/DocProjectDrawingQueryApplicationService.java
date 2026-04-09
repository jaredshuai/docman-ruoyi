package org.dromara.docman.application.service;

import lombok.RequiredArgsConstructor;
import org.dromara.common.core.application.QueryApplicationService;
import org.dromara.docman.domain.vo.DocProjectDrawingVo;
import org.dromara.docman.service.IDocProjectDrawingService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DocProjectDrawingQueryApplicationService implements QueryApplicationService {

    private final IDocProjectDrawingService drawingService;

    public List<DocProjectDrawingVo> listByProject(Long projectId) {
        return drawingService.listByProject(projectId);
    }

    public DocProjectDrawingVo queryById(Long id) {
        return drawingService.queryById(id);
    }
}
