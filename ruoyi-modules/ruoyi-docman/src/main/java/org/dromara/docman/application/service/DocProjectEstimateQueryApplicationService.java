package org.dromara.docman.application.service;

import lombok.RequiredArgsConstructor;
import org.dromara.common.core.application.QueryApplicationService;
import org.dromara.docman.domain.vo.DocProjectEstimateSnapshotVo;
import org.dromara.docman.service.IDocProjectEstimateSnapshotService;
import org.springframework.stereotype.Service;

/**
 * 项目估算查询编排服务。
 */
@Service
@RequiredArgsConstructor
public class DocProjectEstimateQueryApplicationService implements QueryApplicationService {

    private final IDocProjectEstimateSnapshotService estimateSnapshotService;

    /**
     * 查询项目最新估算快照。
     *
     * @param projectId 项目ID
     * @return 最新估算快照
     */
    public DocProjectEstimateSnapshotVo queryLatest(Long projectId) {
        return estimateSnapshotService.queryLatest(projectId);
    }
}
