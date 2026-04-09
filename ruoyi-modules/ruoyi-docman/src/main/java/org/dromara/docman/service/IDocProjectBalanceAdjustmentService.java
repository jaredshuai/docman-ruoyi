package org.dromara.docman.service;

import org.dromara.docman.domain.bo.DocProjectBalanceAdjustmentBo;
import org.dromara.docman.domain.vo.DocProjectBalanceAdjustmentVo;

public interface IDocProjectBalanceAdjustmentService {

    /**
     * 查询项目最新平料结果。
     *
     * @param projectId 项目ID
     * @return 最新平料结果
     */
    DocProjectBalanceAdjustmentVo queryLatest(Long projectId);

    /**
     * 保存项目平料结果。
     *
     * @param bo 平料参数
     * @return 平料记录ID
     */
    Long save(DocProjectBalanceAdjustmentBo bo);
}
