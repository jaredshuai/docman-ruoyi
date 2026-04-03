package org.dromara.docman.mapper;

import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;
import org.dromara.docman.domain.entity.DocProjectAddRecordDetail;
import org.dromara.docman.domain.vo.DocProjectAddRecordDetailVo;

import java.util.List;

public interface DocProjectAddRecordDetailMapper extends BaseMapperPlus<DocProjectAddRecordDetail, DocProjectAddRecordDetailVo> {

    /**
     * 查询指定工作量记录的详情列表
     *
     * @param projectAddRecordId 工作量记录 ID
     * @return 详情列表
     */
    List<DocProjectAddRecordDetailVo> selectVoListByRecordId(Long projectAddRecordId);
}
