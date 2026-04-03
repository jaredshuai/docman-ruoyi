package org.dromara.docman.service;

import org.dromara.docman.domain.bo.DocProjectAddRecordBo;
import org.dromara.docman.domain.vo.DocProjectAddRecordVo;

import java.util.List;

public interface IDocProjectAddRecordService {

    /**
     * 查询项目的工作量记录列表（含详情）
     *
     * @param projectId 项目 ID
     * @return 工作量记录列表
     */
    List<DocProjectAddRecordVo> listByProject(Long projectId);

    /**
     * 查询工作量记录详情
     *
     * @param id 工作量记录 ID
     * @return 工作量记录详情
     */
    DocProjectAddRecordVo queryById(Long id);

    /**
     * 创建或更新工作量记录
     *
     * @param bo 工作量记录参数
     * @return 工作量记录 ID
     */
    Long save(DocProjectAddRecordBo bo);

    /**
     * 删除工作量记录
     *
     * @param ids 工作量记录 ID 列表
     */
    void deleteByIds(List<Long> ids);

    /**
     * 删除指定项目的所有工作量记录
     *
     * @param projectId 项目 ID
     */
    void deleteByProjectId(Long projectId);
}
