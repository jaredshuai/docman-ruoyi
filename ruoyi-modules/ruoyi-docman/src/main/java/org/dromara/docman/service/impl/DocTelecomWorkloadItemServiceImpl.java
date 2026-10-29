package org.dromara.docman.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.docman.domain.bo.DocTelecomWorkloadItemBo;
import org.dromara.docman.domain.entity.DocTelecomWorkloadItem;
import org.dromara.docman.domain.vo.DocTelecomWorkloadItemVo;
import org.dromara.docman.mapper.DocTelecomWorkloadItemMapper;
import org.dromara.docman.service.IDocTelecomWorkloadItemService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DocTelecomWorkloadItemServiceImpl implements IDocTelecomWorkloadItemService {

    private static final BigDecimal DEFAULT_QUANTITY = BigDecimal.ZERO;
    private static final BigDecimal DEFAULT_COEFFICIENT = BigDecimal.ONE;

    private final DocTelecomWorkloadItemMapper workloadItemMapper;

    @Override
    public List<DocTelecomWorkloadItemVo> listAll() {
        return workloadItemMapper.selectVoList(new LambdaQueryWrapper<DocTelecomWorkloadItem>()
            .orderByAsc(DocTelecomWorkloadItem::getSortOrder)
            .orderByAsc(DocTelecomWorkloadItem::getCreateTime));
    }

    @Override
    public DocTelecomWorkloadItemVo queryById(Long id) {
        DocTelecomWorkloadItemVo vo = workloadItemMapper.selectVoById(id);
        if (vo == null) {
            throw new ServiceException("工作量项不存在");
        }
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long save(DocTelecomWorkloadItemBo bo) {
        DocTelecomWorkloadItem entity = new DocTelecomWorkloadItem();
        entity.setId(bo.getId());
        entity.setItemCode(bo.getItemCode());
        entity.setItemName(bo.getItemName());
        entity.setCategory(bo.getCategory());
        entity.setUnit(bo.getUnit());
        entity.setDefaultPrice(bo.getDefaultPrice());
        entity.setTechnician(defaultQuantity(bo.getTechnician()));
        entity.setTechnicianCoefficient(defaultCoefficient(bo.getTechnicianCoefficient()));
        entity.setGeneralWorker(defaultQuantity(bo.getGeneralWorker()));
        entity.setGeneralWorkerCoefficient(defaultCoefficient(bo.getGeneralWorkerCoefficient()));
        entity.setMachineShift(defaultQuantity(bo.getMachineShift()));
        entity.setMachineShiftUnitPrice(defaultQuantity(bo.getMachineShiftUnitPrice()));
        entity.setMachineShiftCoefficient(defaultCoefficient(bo.getMachineShiftCoefficient()));
        entity.setInstrumentShift(defaultQuantity(bo.getInstrumentShift()));
        entity.setInstrumentShiftUnitPrice(defaultQuantity(bo.getInstrumentShiftUnitPrice()));
        entity.setInstrumentShiftCoefficient(defaultCoefficient(bo.getInstrumentShiftCoefficient()));
        entity.setMaterialQuantity(defaultQuantity(bo.getMaterialQuantity()));
        entity.setMaterialUnitPrice(defaultQuantity(bo.getMaterialUnitPrice()));
        entity.setDescription(bo.getDescription());
        entity.setSortOrder(bo.getSortOrder());
        entity.setStatus((bo.getStatus() == null || bo.getStatus().isBlank()) ? "active" : bo.getStatus());
        if (bo.getId() == null) {
            workloadItemMapper.insert(entity);
        } else {
            workloadItemMapper.updateById(entity);
        }
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteByIds(List<Long> ids) {
        if (ids != null) {
            ids.forEach(workloadItemMapper::deleteById);
        }
    }

    private BigDecimal defaultQuantity(BigDecimal value) {
        return value == null ? DEFAULT_QUANTITY : value;
    }

    private BigDecimal defaultCoefficient(BigDecimal value) {
        return value == null ? DEFAULT_COEFFICIENT : value;
    }
}
