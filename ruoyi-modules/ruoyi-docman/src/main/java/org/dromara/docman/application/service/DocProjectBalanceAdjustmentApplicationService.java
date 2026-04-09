package org.dromara.docman.application.service;

import lombok.RequiredArgsConstructor;
import org.dromara.common.core.application.CommandApplicationService;
import org.dromara.docman.domain.bo.DocProjectBalanceAdjustmentBo;
import org.dromara.docman.service.IDocProjectBalanceAdjustmentService;
import org.springframework.stereotype.Service;

/**
 * 项目平料命令编排服务。
 */
@Service
@RequiredArgsConstructor
public class DocProjectBalanceAdjustmentApplicationService implements CommandApplicationService {

    private final IDocProjectBalanceAdjustmentService balanceAdjustmentService;

    /**
     * 保存项目平料结果。
     *
     * @param bo 平料参数
     * @return 平料记录ID
     */
    public Long save(DocProjectBalanceAdjustmentBo bo) {
        return balanceAdjustmentService.save(bo);
    }
}
