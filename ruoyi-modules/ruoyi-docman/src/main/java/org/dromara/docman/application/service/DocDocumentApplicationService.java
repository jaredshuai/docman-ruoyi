package org.dromara.docman.application.service;

import lombok.RequiredArgsConstructor;
import org.dromara.common.core.application.CommandApplicationService;
import org.dromara.docman.domain.bo.DocDocumentRecordBo;
import org.dromara.docman.service.IDocDocumentRecordService;
import org.springframework.stereotype.Service;

/**
 * 文档记录写操作应用服务；查询统一由 {@link DocDocumentQueryApplicationService} 负责
 */
@Service
@RequiredArgsConstructor
public class DocDocumentApplicationService implements CommandApplicationService {

    private final IDocDocumentRecordService documentRecordService;

    public void upload(DocDocumentRecordBo bo) {
        documentRecordService.recordUpload(bo);
    }

    public void markObsolete(Long projectId) {
        documentRecordService.markObsoleteByProjectId(projectId);
    }
}
