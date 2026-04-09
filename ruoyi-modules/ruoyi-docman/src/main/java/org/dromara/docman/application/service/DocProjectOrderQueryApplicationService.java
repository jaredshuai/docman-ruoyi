package org.dromara.docman.application.service;

import lombok.RequiredArgsConstructor;
import org.dromara.common.core.application.QueryApplicationService;
import org.dromara.docman.domain.vo.DocProjectOrderVo;
import org.dromara.docman.service.IDocProjectOrderService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 项目签证单应用服务（读操作）
 */
@Service
@RequiredArgsConstructor
public class DocProjectOrderQueryApplicationService implements QueryApplicationService {

    private final IDocProjectOrderService orderService;

    /**
     * 查询项目的签证单列表
     *
     * @param projectId 项目ID
     * @return 签证单列表
     */
    public List<DocProjectOrderVo> listByProject(Long projectId) {
        return orderService.listByProject(projectId);
    }

    /**
     * 查询签证单详情
     *
     * @param id 签证单ID
     * @return 签证单详情
     */
    public DocProjectOrderVo getById(Long id) {
        return orderService.queryById(id);
    }
}
