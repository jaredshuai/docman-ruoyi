package org.dromara.docman.application.service;

import lombok.RequiredArgsConstructor;
import org.dromara.common.core.application.CommandApplicationService;
import org.dromara.docman.domain.bo.DocProjectOrderBo;
import org.dromara.docman.service.IDocProjectOrderService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 项目签证单应用服务（写操作）
 */
@Service
@RequiredArgsConstructor
public class DocProjectOrderApplicationService implements CommandApplicationService {

    private final IDocProjectOrderService orderService;

    /**
     * 保存签证单
     *
     * @param bo 签证单参数
     * @return 签证单ID
     */
    public Long save(DocProjectOrderBo bo) {
        return orderService.save(bo);
    }

    /**
     * 删除签证单
     *
     * @param ids 签证单ID列表
     */
    public void delete(List<Long> ids) {
        orderService.deleteByIds(ids);
    }
}
