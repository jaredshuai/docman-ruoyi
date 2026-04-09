package org.dromara.docman.service.impl;

import lombok.RequiredArgsConstructor;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.docman.domain.bo.DocProjectOrderBo;
import org.dromara.docman.domain.entity.DocProjectOrder;
import org.dromara.docman.domain.enums.DocProjectAction;
import org.dromara.docman.domain.vo.DocProjectOrderVo;
import org.dromara.docman.mapper.DocProjectOrderMapper;
import org.dromara.docman.service.IDocProjectAccessService;
import org.dromara.docman.service.IDocProjectOrderService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 项目签证单服务实现
 */
@Service
@RequiredArgsConstructor
public class DocProjectOrderServiceImpl implements IDocProjectOrderService {

    private final DocProjectOrderMapper orderMapper;
    private final IDocProjectAccessService projectAccessService;

    @Override
    public List<DocProjectOrderVo> listByProject(Long projectId) {
        projectAccessService.assertAction(projectId, DocProjectAction.VIEW_PROJECT);
        return orderMapper.selectVoListByProjectId(projectId);
    }

    @Override
    public DocProjectOrderVo queryById(Long id) {
        DocProjectOrderVo vo = orderMapper.selectVoById(id);
        if (vo == null) {
            throw new ServiceException("签证单不存在");
        }
        projectAccessService.assertAction(vo.getProjectId(), DocProjectAction.VIEW_PROJECT);
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long save(DocProjectOrderBo bo) {
        projectAccessService.assertAction(bo.getProjectId(), DocProjectAction.EDIT_PROJECT);

        DocProjectOrder order = new DocProjectOrder();
        order.setId(bo.getId());
        order.setProjectId(bo.getProjectId());
        order.setReason(bo.getReason());
        order.setDate(bo.getDate());
        order.setAmount(bo.getAmount());
        order.setRemark(bo.getRemark());

        if (order.getId() == null) {
            orderMapper.insert(order);
        } else {
            orderMapper.updateById(order);
        }
        return order.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteByIds(List<Long> ids) {
        for (Long id : ids) {
            DocProjectOrderVo vo = orderMapper.selectVoById(id);
            if (vo != null) {
                projectAccessService.assertAction(vo.getProjectId(), DocProjectAction.EDIT_PROJECT);
                orderMapper.deleteById(id);
            }
        }
    }
}
