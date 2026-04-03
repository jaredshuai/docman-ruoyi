package org.dromara.docman.service;

import org.dromara.docman.domain.bo.DocProjectAddRecordDetailBo;
import org.dromara.docman.domain.vo.DocProjectAddRecordDetailVo;

import java.util.List;

public interface IDocProjectAddRecordDetailService {

    /**
     * 查询工作量记录的详情列表
     *
     * @param projectAddRecordId 工作量记录 ID
     * @return 详情列表
     */
    List<DocProjectAddRecordDetailVo> listByRecordId(Long projectAddRecordId);

    /**
     * 批量保存工作量明细
     *
     * @param recordId 工作量记录 ID
     * @param details 明细列表
     */
    void saveDetails(Long recordId, List<DocProjectAddRecordDetailBo> details);

    /**
     * 删除工作量明细
     *
     * @param ids 明细 ID 列表
     */
    void deleteByIds(List<Long> ids);

    /**
     * 根据工作量记录 ID 删除所有明细
     *
     * @param projectAddRecordId 工作量记录 ID
     */
    void deleteByRecordId(Long projectAddRecordId);
}
