package org.dromara.docman.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.docman.domain.bo.DocProjectTypeBo;
import org.dromara.docman.domain.entity.DocProjectType;
import org.dromara.docman.domain.vo.DocProjectTypeVo;
import org.dromara.docman.mapper.DocProjectTypeMapper;
import org.dromara.docman.service.IDocProjectTypeService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DocProjectTypeServiceImpl implements IDocProjectTypeService {

    private final DocProjectTypeMapper projectTypeMapper;

    @Override
    public List<DocProjectTypeVo> listAll() {
        List<DocProjectType> entities = projectTypeMapper.selectList(new LambdaQueryWrapper<DocProjectType>()
            .orderByAsc(DocProjectType::getSortOrder)
            .orderByAsc(DocProjectType::getCreateTime));
        List<DocProjectTypeVo> result = new ArrayList<>(entities.size());
        for (DocProjectType entity : entities) {
            result.add(toVo(entity));
        }
        return result;
    }

    @Override
    public DocProjectTypeVo queryById(Long id) {
        DocProjectType entity = projectTypeMapper.selectById(id);
        if (entity == null) {
            throw new ServiceException("项目类型不存在");
        }
        return toVo(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long save(DocProjectTypeBo bo) {
        ensureCodeUnique(bo);
        DocProjectType entity = new DocProjectType();
        entity.setId(bo.getId());
        entity.setCode(bo.getCode());
        entity.setName(bo.getName());
        entity.setCustomerType(bo.getCustomerType());
        entity.setDescription(bo.getDescription());
        entity.setSortOrder(bo.getSortOrder());
        entity.setStatus(defaultStatus(bo.getStatus()));
        if (bo.getId() == null) {
            projectTypeMapper.insert(entity);
        } else {
            projectTypeMapper.updateById(entity);
        }
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteByIds(List<Long> ids) {
        for (Long id : ids) {
            if (projectTypeMapper.selectById(id) != null) {
                projectTypeMapper.deleteById(id);
            }
        }
    }

    private void ensureCodeUnique(DocProjectTypeBo bo) {
        DocProjectType existing = projectTypeMapper.selectOne(new LambdaQueryWrapper<DocProjectType>()
            .eq(DocProjectType::getCode, bo.getCode()));
        if (existing != null && !existing.getId().equals(bo.getId())) {
            throw new ServiceException("项目类型编码已存在");
        }
    }

    private String defaultStatus(String status) {
        return (status == null || status.isBlank()) ? "active" : status;
    }

    private DocProjectTypeVo toVo(DocProjectType entity) {
        DocProjectTypeVo vo = new DocProjectTypeVo();
        vo.setId(entity.getId());
        vo.setCode(entity.getCode());
        vo.setName(entity.getName());
        vo.setCustomerType(entity.getCustomerType());
        vo.setDescription(entity.getDescription());
        vo.setSortOrder(entity.getSortOrder());
        vo.setStatus(entity.getStatus());
        vo.setCreateTime(entity.getCreateTime());
        vo.setUpdateTime(entity.getUpdateTime());
        return vo;
    }
}
