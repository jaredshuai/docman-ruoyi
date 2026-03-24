package org.dromara.docman.application.service;

import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.docman.domain.vo.DocDocumentRecordVo;
import org.dromara.docman.service.IDocDocumentRecordService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("dev")
@Tag("prod")
@Tag("local")
class DocDocumentQueryApplicationServiceTest {

    @Mock
    private IDocDocumentRecordService documentRecordService;

    @InjectMocks
    private DocDocumentQueryApplicationService queryService;

    @Test
    void shouldDelegateListToDocumentRecordService() {
        Long projectId = 1L;
        PageQuery pageQuery = new PageQuery(10, 1);
        TableDataInfo<DocDocumentRecordVo> expectedResult = new TableDataInfo<>();
        when(documentRecordService.queryPageList(projectId, pageQuery)).thenReturn(expectedResult);

        TableDataInfo<DocDocumentRecordVo> result = queryService.list(projectId, pageQuery);

        assertSame(expectedResult, result);
        verify(documentRecordService).queryPageList(projectId, pageQuery);
    }

    @Test
    void shouldDelegateGetByIdToDocumentRecordService() {
        Long id = 1L;
        DocDocumentRecordVo expectedVo = new DocDocumentRecordVo();
        when(documentRecordService.queryById(id)).thenReturn(expectedVo);

        DocDocumentRecordVo result = queryService.getById(id);

        assertSame(expectedVo, result);
        verify(documentRecordService).queryById(id);
    }
}