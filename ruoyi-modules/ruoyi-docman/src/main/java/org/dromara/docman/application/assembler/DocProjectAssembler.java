package org.dromara.docman.application.assembler;

import org.dromara.common.core.application.BaseAssembler;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.docman.domain.bo.DocProjectBo;
import org.dromara.docman.domain.entity.DocProject;
import org.dromara.docman.domain.vo.DocProjectVo;
import org.springframework.stereotype.Component;

@Component
public class DocProjectAssembler implements BaseAssembler<DocProject, DocProjectVo> {

    @Override
    public DocProjectVo toVo(DocProject entity) {
        return MapstructUtils.convert(entity, DocProjectVo.class);
    }

    public DocProject toEntity(DocProjectBo bo) {
        return MapstructUtils.convert(bo, DocProject.class);
    }
}
