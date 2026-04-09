package org.dromara.docman.mapper;

import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;
import org.dromara.docman.domain.entity.DocProjectOrder;
import org.dromara.docman.domain.vo.DocProjectOrderVo;

import java.util.List;

public interface DocProjectOrderMapper extends BaseMapperPlus<DocProjectOrder, DocProjectOrderVo> {

    /**
     * 查询项目的签证单列表
     *
     * @param projectId 项目ID
     * @return 签证单列表
     */
    List<DocProjectOrderVo> selectVoListByProjectId(Long projectId);
}
