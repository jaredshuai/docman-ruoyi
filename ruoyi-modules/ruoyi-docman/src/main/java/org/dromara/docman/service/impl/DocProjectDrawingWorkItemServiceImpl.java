package org.dromara.docman.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.docman.domain.bo.DocProjectDrawingWorkItemBo;
import org.dromara.docman.domain.entity.DocProjectDrawing;
import org.dromara.docman.domain.entity.DocProjectDrawingWorkItem;
import org.dromara.docman.domain.enums.DocProjectAction;
import org.dromara.docman.domain.vo.DocProjectDrawingWorkItemVo;
import org.dromara.docman.mapper.DocProjectDrawingMapper;
import org.dromara.docman.mapper.DocProjectDrawingWorkItemMapper;
import org.dromara.docman.service.IDocProjectAccessService;
import org.dromara.docman.service.IDocProjectDrawingWorkItemService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DocProjectDrawingWorkItemServiceImpl implements IDocProjectDrawingWorkItemService {

    private final DocProjectDrawingWorkItemMapper workItemMapper;
    private final DocProjectDrawingMapper drawingMapper;
    private final IDocProjectAccessService projectAccessService;

    @Override
    public List<DocProjectDrawingWorkItemVo> listByProject(Long projectId) {
        projectAccessService.assertAction(projectId, DocProjectAction.VIEW_PROJECT);
        return workItemMapper.selectVoList(new LambdaQueryWrapper<DocProjectDrawingWorkItem>()
            .eq(DocProjectDrawingWorkItem::getProjectId, projectId)
            .orderByAsc(DocProjectDrawingWorkItem::getDrawingId)
            .orderByAsc(DocProjectDrawingWorkItem::getCreateTime));
    }

    @Override
    public List<DocProjectDrawingWorkItemVo> listByDrawing(Long projectId, Long drawingId) {
        projectAccessService.assertAction(projectId, DocProjectAction.VIEW_PROJECT);
        return workItemMapper.selectVoList(new LambdaQueryWrapper<DocProjectDrawingWorkItem>()
            .eq(DocProjectDrawingWorkItem::getProjectId, projectId)
            .eq(DocProjectDrawingWorkItem::getDrawingId, drawingId)
            .orderByAsc(DocProjectDrawingWorkItem::getCreateTime));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long save(DocProjectDrawingWorkItemBo bo) {
        projectAccessService.assertAction(bo.getProjectId(), DocProjectAction.EDIT_PROJECT);
        if (bo.getId() != null) {
            DocProjectDrawingWorkItem existing = workItemMapper.selectById(bo.getId());
            if (existing == null) {
                throw new ServiceException("图纸工作量映射不存在");
            }
            if (!bo.getProjectId().equals(existing.getProjectId())) {
                throw new ServiceException("图纸工作量映射不属于当前项目");
            }
        }
        DocProjectDrawing drawing = drawingMapper.selectById(bo.getDrawingId());
        if (drawing == null || !bo.getProjectId().equals(drawing.getProjectId())) {
            throw new ServiceException("图纸记录不存在");
        }
        DocProjectDrawingWorkItem entity = new DocProjectDrawingWorkItem();
        entity.setId(bo.getId());
        entity.setProjectId(bo.getProjectId());
        entity.setDrawingId(bo.getDrawingId());
        entity.setWorkItemCode(bo.getWorkItemCode());
        entity.setWorkItemName(bo.getWorkItemName());
        entity.setCategory(bo.getCategory());
        entity.setUnit(bo.getUnit());
        entity.setQuantity(bo.getQuantity());
        entity.setIncludeInEstimate(Boolean.TRUE.equals(bo.getIncludeInEstimate()));
        entity.setRemark(bo.getRemark());
        if (bo.getId() == null) {
            workItemMapper.insert(entity);
        } else {
            workItemMapper.updateById(entity);
        }
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteByIds(List<Long> ids) {
        if (ids != null) {
            for (Long id : ids) {
                DocProjectDrawingWorkItem entity = workItemMapper.selectById(id);
                if (entity != null) {
                    projectAccessService.assertAction(entity.getProjectId(), DocProjectAction.EDIT_PROJECT);
                    workItemMapper.deleteById(id);
                }
            }
        }
    }
}
