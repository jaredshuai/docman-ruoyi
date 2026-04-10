package org.dromara.docman.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.dromara.docman.domain.entity.DocProjectEstimateSnapshot;
import org.dromara.docman.domain.vo.DocProjectEstimateSnapshotVo;
import org.dromara.docman.mapper.DocProjectEstimateSnapshotMapper;
import org.dromara.docman.service.IDocProjectEstimateSnapshotService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class DocProjectEstimateSnapshotServiceImpl implements IDocProjectEstimateSnapshotService {

    private final DocProjectEstimateSnapshotMapper estimateSnapshotMapper;

    @Override
    public DocProjectEstimateSnapshotVo queryLatest(Long projectId) {
        DocProjectEstimateSnapshot snapshot = estimateSnapshotMapper.selectOne(new LambdaQueryWrapper<DocProjectEstimateSnapshot>()
            .eq(DocProjectEstimateSnapshot::getProjectId, projectId)
            .orderByDesc(DocProjectEstimateSnapshot::getCreateTime)
            .last("limit 1"));
        return snapshot == null ? null : toVo(snapshot);
    }

    @Override
    public Long saveSnapshot(Long projectId, String estimateType, BigDecimal estimateAmount, Long drawingCount,
                             Long visaCount, String status, String summary) {
        DocProjectEstimateSnapshot snapshot = new DocProjectEstimateSnapshot();
        snapshot.setProjectId(projectId);
        snapshot.setEstimateType(estimateType);
        snapshot.setEstimateAmount(estimateAmount);
        snapshot.setDrawingCount(drawingCount);
        snapshot.setVisaCount(visaCount);
        snapshot.setStatus(status);
        snapshot.setSummary(summary);
        estimateSnapshotMapper.insert(snapshot);
        return snapshot.getId();
    }

    private DocProjectEstimateSnapshotVo toVo(DocProjectEstimateSnapshot snapshot) {
        DocProjectEstimateSnapshotVo vo = new DocProjectEstimateSnapshotVo();
        vo.setId(snapshot.getId());
        vo.setProjectId(snapshot.getProjectId());
        vo.setEstimateType(snapshot.getEstimateType());
        vo.setEstimateAmount(snapshot.getEstimateAmount());
        vo.setDrawingCount(snapshot.getDrawingCount());
        vo.setVisaCount(snapshot.getVisaCount());
        vo.setStatus(snapshot.getStatus());
        vo.setSummary(snapshot.getSummary());
        vo.setCreateTime(snapshot.getCreateTime());
        return vo;
    }
}
