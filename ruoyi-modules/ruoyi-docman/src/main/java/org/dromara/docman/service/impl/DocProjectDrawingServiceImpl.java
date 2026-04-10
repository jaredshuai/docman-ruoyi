package org.dromara.docman.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.docman.domain.bo.DocProjectDrawingBo;
import org.dromara.docman.domain.entity.DocProjectDrawing;
import org.dromara.docman.domain.enums.DocProjectAction;
import org.dromara.docman.domain.vo.DocProjectDrawingVo;
import org.dromara.docman.mapper.DocProjectDrawingMapper;
import org.dromara.docman.service.IDocProjectAccessService;
import org.dromara.docman.service.IDocProjectDrawingService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DocProjectDrawingServiceImpl implements IDocProjectDrawingService {

    private final DocProjectDrawingMapper drawingMapper;
    private final IDocProjectAccessService projectAccessService;

    @Override
    public List<DocProjectDrawingVo> listByProject(Long projectId) {
        projectAccessService.assertAction(projectId, DocProjectAction.VIEW_PROJECT);
        List<DocProjectDrawing> entities = drawingMapper.selectList(new LambdaQueryWrapper<DocProjectDrawing>()
            .eq(DocProjectDrawing::getProjectId, projectId)
            .orderByAsc(DocProjectDrawing::getCreateTime));
        List<DocProjectDrawingVo> result = new ArrayList<>(entities.size());
        for (DocProjectDrawing entity : entities) {
            result.add(toVo(entity));
        }
        return result;
    }

    @Override
    public DocProjectDrawingVo queryById(Long id) {
        DocProjectDrawing entity = drawingMapper.selectById(id);
        if (entity == null) {
            throw new ServiceException("图纸记录不存在");
        }
        projectAccessService.assertAction(entity.getProjectId(), DocProjectAction.VIEW_PROJECT);
        return toVo(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long save(DocProjectDrawingBo bo) {
        projectAccessService.assertAction(bo.getProjectId(), DocProjectAction.EDIT_PROJECT);
        if (bo.getId() != null) {
            DocProjectDrawing existing = drawingMapper.selectById(bo.getId());
            if (existing == null) {
                throw new ServiceException("图纸记录不存在");
            }
            if (!bo.getProjectId().equals(existing.getProjectId())) {
                throw new ServiceException("图纸记录不属于当前项目");
            }
        }
        DocProjectDrawing entity = new DocProjectDrawing();
        entity.setId(bo.getId());
        entity.setProjectId(bo.getProjectId());
        entity.setDrawingCode(bo.getDrawingCode());
        entity.setOrderSerialNo(bo.getOrderSerialNo());
        entity.setWorkContent(bo.getWorkContent());
        entity.setIncludeInProject(Boolean.TRUE.equals(bo.getIncludeInProject()));
        entity.setRemark(bo.getRemark());
        if (bo.getId() == null) {
            drawingMapper.insert(entity);
        } else {
            drawingMapper.updateById(entity);
        }
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteByIds(List<Long> ids) {
        for (Long id : ids) {
            DocProjectDrawing entity = drawingMapper.selectById(id);
            if (entity != null) {
                projectAccessService.assertAction(entity.getProjectId(), DocProjectAction.EDIT_PROJECT);
                drawingMapper.deleteById(id);
            }
        }
    }

    private DocProjectDrawingVo toVo(DocProjectDrawing entity) {
        DocProjectDrawingVo vo = new DocProjectDrawingVo();
        vo.setId(entity.getId());
        vo.setProjectId(entity.getProjectId());
        vo.setDrawingCode(entity.getDrawingCode());
        vo.setOrderSerialNo(entity.getOrderSerialNo());
        vo.setWorkContent(entity.getWorkContent());
        vo.setIncludeInProject(entity.getIncludeInProject());
        vo.setRemark(entity.getRemark());
        vo.setCreateTime(entity.getCreateTime());
        vo.setUpdateTime(entity.getUpdateTime());
        return vo;
    }
}
