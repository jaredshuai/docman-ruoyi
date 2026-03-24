package org.dromara.docman.application.service;

import lombok.RequiredArgsConstructor;
import org.dromara.common.core.application.QueryApplicationService;
import org.dromara.docman.domain.vo.DocNodeDeadlineVo;
import org.dromara.docman.service.IDocNodeDeadlineService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DocNodeDeadlineQueryApplicationService implements QueryApplicationService {

    private final IDocNodeDeadlineService nodeDeadlineService;

    public List<DocNodeDeadlineVo> listByProject(Long projectId) {
        return nodeDeadlineService.listByProject(projectId);
    }
}
