package org.dromara.docman.application.service;

import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.docman.domain.bo.DocProjectBo;
import org.dromara.docman.domain.vo.DocProjectVo;
import org.dromara.docman.service.IDocProjectService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("dev")
@Tag("prod")
@Tag("local")
class DocProjectQueryApplicationServiceTest {

    @Mock
    private IDocProjectService projectService;

    @InjectMocks
    private DocProjectQueryApplicationService queryService;

    @Test
    void shouldDelegateListToProjectService() {
        DocProjectBo bo = new DocProjectBo();
        PageQuery pageQuery = new PageQuery(10, 1);
        TableDataInfo<DocProjectVo> expectedResult = new TableDataInfo<>();
        when(projectService.queryPageList(bo, pageQuery)).thenReturn(expectedResult);

        TableDataInfo<DocProjectVo> result = queryService.list(bo, pageQuery);

        assertSame(expectedResult, result);
        verify(projectService).queryPageList(bo, pageQuery);
    }

    @Test
    void shouldDelegateGetByIdToProjectService() {
        Long id = 1L;
        DocProjectVo expectedVo = new DocProjectVo();
        when(projectService.queryById(id)).thenReturn(expectedVo);

        DocProjectVo result = queryService.getById(id);

        assertSame(expectedVo, result);
        verify(projectService).queryById(id);
    }

    @Test
    void shouldDelegateMyListToProjectService() {
        DocProjectBo bo = new DocProjectBo();
        java.util.List<DocProjectVo> expectedResult = java.util.List.of(new DocProjectVo());
        when(projectService.queryMyList(bo)).thenReturn(expectedResult);

        java.util.List<DocProjectVo> result = queryService.listMy(bo);

        assertSame(expectedResult, result);
        verify(projectService).queryMyList(bo);
    }
}
