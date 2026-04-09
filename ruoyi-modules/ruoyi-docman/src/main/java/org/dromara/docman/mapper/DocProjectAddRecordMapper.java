package org.dromara.docman.mapper;

import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;
import org.dromara.docman.domain.entity.DocProjectAddRecord;
import org.dromara.docman.domain.vo.DocProjectAddRecordVo;

import java.util.List;

public interface DocProjectAddRecordMapper extends BaseMapperPlus<DocProjectAddRecord, DocProjectAddRecordVo> {

    /**
     * 查询项目的工作量记录列表（含详情）
     *
     * @param projectId 项目 ID
     * @return 工作量记录列表
     */
    List<DocProjectAddRecordVo> selectVoListByProjectId(Long projectId);

    /**
     * 查询工作量记录视图详情。
     *
     * @param id 工作量记录ID
     * @return 工作量记录视图
     */
    DocProjectAddRecordVo selectVoViewById(Long id);
}
