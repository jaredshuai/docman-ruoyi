package org.dromara.docman.application.assembler;

import org.dromara.common.core.application.BaseAssembler;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.docman.domain.entity.DocProjectMember;
import org.dromara.docman.domain.vo.DocProjectMemberVo;
import org.springframework.stereotype.Component;

@Component
public class DocProjectMemberAssembler implements BaseAssembler<DocProjectMember, DocProjectMemberVo> {

    @Override
    public DocProjectMemberVo toVo(DocProjectMember entity) {
        return MapstructUtils.convert(entity, DocProjectMemberVo.class);
    }
}
