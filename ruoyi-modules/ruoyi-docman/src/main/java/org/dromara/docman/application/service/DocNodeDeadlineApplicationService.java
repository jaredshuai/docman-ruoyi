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

    /**
     * 更新节点截止日期配置。
     *
     * @param bo 截止日期变更参数
     */
    public void update(DocNodeDeadlineBo bo) {
        nodeDeadlineService.updateDeadline(bo);
    }
}
