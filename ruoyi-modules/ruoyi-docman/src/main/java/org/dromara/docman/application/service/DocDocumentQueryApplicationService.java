package org.dromara.docman.application.service;

import lombok.RequiredArgsConstructor;
import org.dromara.common.core.application.QueryApplicationService;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.docman.domain.vo.DocDocumentRecordVo;
import org.dromara.docman.service.IDocDocumentRecordService;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DocDocumentQueryApplicationService implements QueryApplicationService {

    private final IDocDocumentRecordService documentRecordService;

    public TableDataInfo<DocDocumentRecordVo> list(Long projectId, PageQuery pageQuery) {
        return documentRecordService.queryPageList(projectId, pageQuery);
    }

    public DocDocumentRecordVo getById(Long id) {
        return documentRecordService.queryById(id);
    }
}
