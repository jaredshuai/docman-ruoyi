package org.dromara.docman.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.dromara.docman.domain.bo.DocProjectAddRecordDetailBo;
import org.dromara.docman.domain.entity.DocProjectAddRecordDetail;
import org.dromara.docman.domain.vo.DocProjectAddRecordDetailVo;
import org.dromara.docman.mapper.DocProjectAddRecordDetailMapper;
import org.dromara.docman.service.IDocProjectAddRecordDetailService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DocProjectAddRecordDetailServiceImpl implements IDocProjectAddRecordDetailService {

    private final DocProjectAddRecordDetailMapper detailMapper;

    @Override
    public List<DocProjectAddRecordDetailVo> listByRecordId(Long projectAddRecordId) {
        return detailMapper.selectVoListByRecordId(projectAddRecordId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveDetails(Long recordId, List<DocProjectAddRecordDetailBo> details) {
        if (details == null || details.isEmpty()) {
            return;
        }

        List<DocProjectAddRecordDetail> detailEntities = new ArrayList<>();
        for (DocProjectAddRecordDetailBo bo : details) {
            DocProjectAddRecordDetail detail = new DocProjectAddRecordDetail();
            detail.setProjectId(bo.getProjectId());
            detail.setProjectAddRecordId(recordId);
            detail.setName(bo.getName());
            detail.setAlias(bo.getAlias());
            detail.setPrice(bo.getPrice());
            detail.setRemark(bo.getRemark());
            detail.setCreateTime(new Date());
            detailEntities.add(detail);
        }

        for (DocProjectAddRecordDetail detail : detailEntities) {
            detailMapper.insert(detail);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteByIds(List<Long> ids) {
        if (ids != null && !ids.isEmpty()) {
            detailMapper.deleteBatchIds(ids);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteByRecordId(Long projectAddRecordId) {
        detailMapper.delete(
            new LambdaQueryWrapper<DocProjectAddRecordDetail>()
                .eq(DocProjectAddRecordDetail::getProjectAddRecordId, projectAddRecordId)
        );
    }
}
