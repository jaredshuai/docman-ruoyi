package org.dromara.docman.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.docman.domain.bo.DocProjectBalanceAdjustmentBo;
import org.dromara.docman.domain.entity.DocProjectBalanceAdjustment;
import org.dromara.docman.domain.entity.DocProjectEstimateSnapshot;
import org.dromara.docman.domain.enums.DocProjectAction;
import org.dromara.docman.domain.vo.DocProjectBalanceAdjustmentVo;
import org.dromara.docman.mapper.DocProjectBalanceAdjustmentMapper;
import org.dromara.docman.mapper.DocProjectEstimateSnapshotMapper;
import org.dromara.docman.service.IDocProjectAccessService;
import org.dromara.docman.service.IDocProjectBalanceAdjustmentService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DocProjectBalanceAdjustmentServiceImpl implements IDocProjectBalanceAdjustmentService {

    private static final String INITIAL_ESTIMATE_TYPE = "initial_estimate";

    private final DocProjectBalanceAdjustmentMapper balanceAdjustmentMapper;
    private final DocProjectEstimateSnapshotMapper estimateSnapshotMapper;
    private final IDocProjectAccessService projectAccessService;

    @Override
    public DocProjectBalanceAdjustmentVo queryLatest(Long projectId) {
        projectAccessService.assertAction(projectId, DocProjectAction.VIEW_PROJECT);
        return balanceAdjustmentMapper.selectVoOne(new LambdaQueryWrapper<DocProjectBalanceAdjustment>()
            .eq(DocProjectBalanceAdjustment::getProjectId, projectId)
            .orderByDesc(DocProjectBalanceAdjustment::getCreateTime)
            .last("limit 1"));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long save(DocProjectBalanceAdjustmentBo bo) {
        projectAccessService.assertAction(bo.getProjectId(), DocProjectAction.EDIT_PROJECT);
        ensureEstimateSnapshotExists(bo.getProjectId());
        validateUpdateScope(bo);
        DocProjectBalanceAdjustment entity = new DocProjectBalanceAdjustment();
        entity.setProjectId(bo.getProjectId());
        entity.setMaterialPrice(bo.getMaterialPrice());
        entity.setBalanceRemark(bo.getBalanceRemark());
        entity.setStatus((bo.getStatus() == null || bo.getStatus().isBlank()) ? "active" : bo.getStatus());
        balanceAdjustmentMapper.insert(entity);
        return entity.getId();
    }

    private void ensureEstimateSnapshotExists(Long projectId) {
        Long count = estimateSnapshotMapper.selectCount(new LambdaQueryWrapper<DocProjectEstimateSnapshot>()
            .eq(DocProjectEstimateSnapshot::getProjectId, projectId)
            .eq(DocProjectEstimateSnapshot::getEstimateType, INITIAL_ESTIMATE_TYPE));
        if (count == null || count <= 0) {
            throw new ServiceException("请先完成初步估算后再执行平料");
        }
    }

    private void validateUpdateScope(DocProjectBalanceAdjustmentBo bo) {
        if (bo.getId() == null) {
            return;
        }
        DocProjectBalanceAdjustment existing = balanceAdjustmentMapper.selectById(bo.getId());
        if (existing == null) {
            throw new ServiceException("平料记录不存在");
        }
        if (!bo.getProjectId().equals(existing.getProjectId())) {
            throw new ServiceException("不允许跨项目修改平料记录");
        }
    }
}
