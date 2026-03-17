package org.dromara.docman.application.assembler;

import org.dromara.common.core.application.BaseAssembler;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.docman.domain.entity.DocProcessConfig;
import org.dromara.docman.domain.vo.DocProcessConfigVo;
import org.springframework.stereotype.Component;

@Component
public class DocProcessAssembler implements BaseAssembler<DocProcessConfig, DocProcessConfigVo> {

    @Override
    public DocProcessConfigVo toVo(DocProcessConfig processConfig) {
        return MapstructUtils.convert(processConfig, DocProcessConfigVo.class);
    }
}
