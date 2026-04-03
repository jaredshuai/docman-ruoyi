package org.dromara.docman.application.assembler;

import org.dromara.common.core.application.BaseAssembler;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.docman.domain.bo.DocProjectAddRecordDetailBo;
import org.dromara.docman.domain.entity.DocProjectAddRecordDetail;
import org.dromara.docman.domain.vo.DocProjectAddRecordDetailVo;
import org.springframework.stereotype.Component;

@Component
public class DocProjectAddRecordDetailAssembler implements BaseAssembler<DocProjectAddRecordDetail, DocProjectAddRecordDetailVo> {

    @Override
    public DocProjectAddRecordDetailVo toVo(DocProjectAddRecordDetail entity) {
        return MapstructUtils.convert(entity, DocProjectAddRecordDetailVo.class);
    }

    public DocProjectAddRecordDetail toEntity(DocProjectAddRecordDetailBo bo) {
        return MapstructUtils.convert(bo, DocProjectAddRecordDetail.class);
    }
}
