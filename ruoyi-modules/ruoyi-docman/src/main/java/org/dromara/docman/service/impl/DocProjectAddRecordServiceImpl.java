package org.dromara.docman.service.impl;

import lombok.RequiredArgsConstructor;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.docman.domain.bo.DocProjectAddRecordBo;
import org.dromara.docman.domain.entity.DocProjectAddRecord;
import org.dromara.docman.domain.enums.DocProjectAction;
import org.dromara.docman.domain.vo.DocProjectAddRecordDetailVo;
import org.dromara.docman.domain.vo.DocProjectAddRecordVo;
import org.dromara.docman.mapper.DocProjectAddRecordMapper;
import org.dromara.docman.service.IDocProjectAccessService;
import org.dromara.docman.service.IDocProjectAddRecordDetailService;
import org.dromara.docman.service.IDocProjectAddRecordService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DocProjectAddRecordServiceImpl implements IDocProjectAddRecordService {

    private final DocProjectAddRecordMapper addRecordMapper;
    private final IDocProjectAccessService projectAccessService;
    private final IDocProjectAddRecordDetailService detailService;

    @Override
    public List<DocProjectAddRecordVo> listByProject(Long projectId) {
        projectAccessService.assertAction(projectId, DocProjectAction.VIEW_PROJECT);
        return addRecordMapper.selectVoListByProjectId(projectId);
    }

    @Override
    public DocProjectAddRecordVo queryById(Long id) {
        DocProjectAddRecordVo vo = addRecordMapper.selectVoViewById(id);
        if (vo == null) {
            throw new ServiceException("工作量记录不存在");
        }
        projectAccessService.assertAction(vo.getProjectId(), DocProjectAction.VIEW_PROJECT);
        List<DocProjectAddRecordDetailVo> details = detailService.listByRecordId(id);
        vo.setDetails(details);
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long save(DocProjectAddRecordBo bo) {
        projectAccessService.assertAction(bo.getProjectId(), DocProjectAction.EDIT_PROJECT);

        DocProjectAddRecord record = new DocProjectAddRecord();
        record.setId(bo.getId());
        record.setProjectId(bo.getProjectId());
        record.setEnable(bo.getEnable());
        record.setEstimatedPrice(bo.getEstimatedPrice());
        record.setRemark(bo.getRemark());

        if (bo.getId() == null) {
            addRecordMapper.insert(record);
        } else {
            addRecordMapper.updateById(record);
        }

        Long recordId = record.getId();
        if (bo.getDetails() != null) {
            detailService.deleteByRecordId(recordId);
            detailService.saveDetails(recordId, bo.getDetails());
        }

        return recordId;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteByIds(List<Long> ids) {
        for (Long id : ids) {
            DocProjectAddRecordVo vo = addRecordMapper.selectVoViewById(id);
            if (vo != null) {
                projectAccessService.assertAction(vo.getProjectId(), DocProjectAction.DELETE_PROJECT);
                detailService.deleteByRecordId(id);
                addRecordMapper.deleteById(id);
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteByProjectId(Long projectId) {
        List<DocProjectAddRecordVo> records = listByProject(projectId);
        for (DocProjectAddRecordVo vo : records) {
            detailService.deleteByRecordId(vo.getId());
            addRecordMapper.deleteById(vo.getId());
        }
    }
}
