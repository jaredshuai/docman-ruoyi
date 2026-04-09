package org.dromara.docman.application.service;

import lombok.RequiredArgsConstructor;
import org.dromara.common.core.application.CommandApplicationService;
import org.dromara.docman.domain.bo.DocProjectDrawingBo;
import org.dromara.docman.service.IDocProjectDrawingService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DocProjectDrawingApplicationService implements CommandApplicationService {

    private final IDocProjectDrawingService drawingService;

    public Long save(DocProjectDrawingBo bo) {
        return drawingService.save(bo);
    }

    public void delete(List<Long> ids) {
        drawingService.deleteByIds(ids);
    }
}
