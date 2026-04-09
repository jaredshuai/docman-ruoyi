package org.dromara.docman.service;

import org.dromara.docman.domain.vo.DocProjectEstimateSnapshotVo;

import java.math.BigDecimal;

public interface IDocProjectEstimateSnapshotService {

    /**
     * 查询项目最近一次估算快照。
     *
     * @param projectId 项目ID
     * @return 最近一次估算快照
     */
    DocProjectEstimateSnapshotVo queryLatest(Long projectId);

    /**
     * 保存一条新的估算快照。
     *
     * @param projectId       项目ID
     * @param estimateType    估算类型
     * @param estimateAmount  估算金额
     * @param drawingCount    图纸数量
     * @param visaCount       签证数量
     * @param status          估算状态
     * @param summary         摘要
     * @return 快照ID
     */
    Long saveSnapshot(Long projectId, String estimateType, BigDecimal estimateAmount, Long drawingCount,
                      Long visaCount, String status, String summary);
}
