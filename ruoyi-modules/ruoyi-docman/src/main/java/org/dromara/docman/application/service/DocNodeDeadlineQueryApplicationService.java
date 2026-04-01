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

    /**
     * 查询项目下的节点截止日期列表。
     *
     * @param projectId 项目ID
     * @return 节点截止日期列表
     */
    public List<DocNodeDeadlineVo> listByProject(Long projectId) {
        return nodeDeadlineService.listByProject(projectId);
    }
}
