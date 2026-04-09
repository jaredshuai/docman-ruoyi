package org.dromara.docman.service;

import org.dromara.docman.domain.bo.DocProjectOrderBo;
import org.dromara.docman.domain.vo.DocProjectOrderVo;

import java.util.List;

/**
 * 项目签证单服务接口
 */
public interface IDocProjectOrderService {

    /**
     * 查询项目的签证单列表
     *
     * @param projectId 项目ID
     * @return 签证单列表
     */
    List<DocProjectOrderVo> listByProject(Long projectId);

    /**
     * 查询签证单详情
     *
     * @param id 签证单ID
     * @return 签证单详情
     */
    DocProjectOrderVo queryById(Long id);

    /**
     * 创建或更新签证单
     *
     * @param bo 签证单参数
     * @return 签证单ID
     */
    Long save(DocProjectOrderBo bo);

    /**
     * 删除签证单
     *
     * @param ids 签证单ID列表
     */
    void deleteByIds(List<Long> ids);
}
