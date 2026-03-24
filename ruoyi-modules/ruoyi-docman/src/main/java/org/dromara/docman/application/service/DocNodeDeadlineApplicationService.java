package org.dromara.docman.application.service;

import lombok.RequiredArgsConstructor;
import org.dromara.common.core.application.CommandApplicationService;
import org.dromara.docman.domain.bo.DocNodeDeadlineBo;
import org.dromara.docman.service.IDocNodeDeadlineService;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DocNodeDeadlineApplicationService implements CommandApplicationService {

    private final IDocNodeDeadlineService nodeDeadlineService;

    public void update(DocNodeDeadlineBo bo) {
        nodeDeadlineService.updateDeadline(bo);
    }
}
