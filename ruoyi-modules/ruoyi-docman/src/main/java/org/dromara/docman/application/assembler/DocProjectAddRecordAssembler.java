package org.dromara.docman.application.assembler;

import org.dromara.common.core.application.BaseAssembler;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.docman.domain.bo.DocProjectAddRecordBo;
import org.dromara.docman.domain.entity.DocProjectAddRecord;
import org.dromara.docman.domain.vo.DocProjectAddRecordVo;
import org.springframework.stereotype.Component;

@Component
public class DocProjectAddRecordAssembler implements BaseAssembler<DocProjectAddRecord, DocProjectAddRecordVo> {

    @Override
    public DocProjectAddRecordVo toVo(DocProjectAddRecord entity) {
        return MapstructUtils.convert(entity, DocProjectAddRecordVo.class);
    }

    public DocProjectAddRecord toEntity(DocProjectAddRecordBo bo) {
        return MapstructUtils.convert(bo, DocProjectAddRecord.class);
    }
}
