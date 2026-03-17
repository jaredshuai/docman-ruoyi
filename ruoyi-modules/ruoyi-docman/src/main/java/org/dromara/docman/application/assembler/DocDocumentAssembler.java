package org.dromara.docman.application.assembler;

import org.dromara.common.core.application.BaseAssembler;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.docman.domain.bo.DocDocumentRecordBo;
import org.dromara.docman.domain.entity.DocDocumentRecord;
import org.dromara.docman.domain.vo.DocDocumentRecordVo;
import org.springframework.stereotype.Component;

@Component
public class DocDocumentAssembler implements BaseAssembler<DocDocumentRecord, DocDocumentRecordVo> {

    @Override
    public DocDocumentRecordVo toVo(DocDocumentRecord entity) {
        return MapstructUtils.convert(entity, DocDocumentRecordVo.class);
    }

    public DocDocumentRecord toEntity(DocDocumentRecordBo bo) {
        return MapstructUtils.convert(bo, DocDocumentRecord.class);
    }
}
