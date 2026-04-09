package org.dromara.docman.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.docman.domain.bo.DocProjectVisaBo;
import org.dromara.docman.domain.entity.DocProjectVisa;
import org.dromara.docman.domain.enums.DocProjectAction;
import org.dromara.docman.domain.vo.DocProjectVisaVo;
import org.dromara.docman.mapper.DocProjectVisaMapper;
import org.dromara.docman.service.IDocProjectAccessService;
import org.dromara.docman.service.IDocProjectVisaService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DocProjectVisaServiceImpl implements IDocProjectVisaService {

    private final DocProjectVisaMapper visaMapper;
    private final IDocProjectAccessService projectAccessService;

    @Override
    public List<DocProjectVisaVo> listByProject(Long projectId) {
        projectAccessService.assertAction(projectId, DocProjectAction.VIEW_PROJECT);
        return visaMapper.selectVoList(new LambdaQueryWrapper<DocProjectVisa>()
            .eq(DocProjectVisa::getProjectId, projectId)
            .orderByAsc(DocProjectVisa::getCreateTime));
    }

    @Override
    public DocProjectVisaVo queryById(Long id) {
        DocProjectVisa entity = visaMapper.selectById(id);
        if (entity == null) {
            throw new ServiceException("签证记录不存在");
        }
        projectAccessService.assertAction(entity.getProjectId(), DocProjectAction.VIEW_PROJECT);
        return toVo(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long save(DocProjectVisaBo bo) {
        projectAccessService.assertAction(bo.getProjectId(), DocProjectAction.EDIT_PROJECT);
        if (bo.getId() != null) {
            DocProjectVisa existing = visaMapper.selectById(bo.getId());
            if (existing == null) {
                throw new ServiceException("签证记录不存在");
            }
            if (!bo.getProjectId().equals(existing.getProjectId())) {
                throw new ServiceException("签证记录不属于当前项目");
            }
        }
        DocProjectVisa entity = new DocProjectVisa();
        entity.setId(bo.getId());
        entity.setProjectId(bo.getProjectId());
        entity.setReason(bo.getReason());
        entity.setContentBasis(bo.getContentBasis());
        entity.setAmount(bo.getAmount());
        entity.setVisaDate(bo.getVisaDate());
        entity.setIncludeInProject(Boolean.TRUE.equals(bo.getIncludeInProject()));
        entity.setRemark(bo.getRemark());
        if (bo.getId() == null) {
            visaMapper.insert(entity);
        } else {
            visaMapper.updateById(entity);
        }
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteByIds(List<Long> ids) {
        for (Long id : ids) {
            DocProjectVisa entity = visaMapper.selectById(id);
            if (entity != null) {
                projectAccessService.assertAction(entity.getProjectId(), DocProjectAction.EDIT_PROJECT);
                visaMapper.deleteById(id);
            }
        }
    }

    private DocProjectVisaVo toVo(DocProjectVisa entity) {
        DocProjectVisaVo vo = new DocProjectVisaVo();
        vo.setId(entity.getId());
        vo.setProjectId(entity.getProjectId());
        vo.setReason(entity.getReason());
        vo.setContentBasis(entity.getContentBasis());
        vo.setAmount(entity.getAmount());
        vo.setVisaDate(entity.getVisaDate());
        vo.setIncludeInProject(entity.getIncludeInProject());
        vo.setRemark(entity.getRemark());
        vo.setCreateTime(entity.getCreateTime());
        vo.setUpdateTime(entity.getUpdateTime());
        return vo;
    }
}
