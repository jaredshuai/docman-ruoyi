package org.dromara.docman.application.service;

import lombok.RequiredArgsConstructor;
import org.dromara.common.core.application.QueryApplicationService;
import org.dromara.docman.domain.vo.DocProjectBalanceAdjustmentVo;
import org.dromara.docman.service.IDocProjectBalanceAdjustmentService;
import org.springframework.stereotype.Service;

/**
 * 项目平料查询编排服务。
 */
@Service
@RequiredArgsConstructor
public class DocProjectBalanceAdjustmentQueryApplicationService implements QueryApplicationService {

    private final IDocProjectBalanceAdjustmentService balanceAdjustmentService;

    /**
     * 查询项目最新平料结果。
     *
     * @param projectId 项目ID
     * @return 最新平料结果
     */
    public DocProjectBalanceAdjustmentVo queryLatest(Long projectId) {
        return balanceAdjustmentService.queryLatest(projectId);
    }
}
