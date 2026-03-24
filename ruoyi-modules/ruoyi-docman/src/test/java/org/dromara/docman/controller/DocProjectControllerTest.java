package org.dromara.docman.controller;

import org.dromara.common.core.domain.R;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.docman.application.service.DocProjectApplicationService;
import org.dromara.docman.application.service.DocProjectQueryApplicationService;
import org.dromara.docman.domain.bo.DocProjectBo;
import org.dromara.docman.domain.vo.DocProjectVo;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("dev")
@Tag("prod")
@Tag("local")
class DocProjectControllerTest {

    @Mock
    private DocProjectApplicationService projectApplicationService;

    @Mock
    private DocProjectQueryApplicationService projectQueryApplicationService;

    @Test
    void shouldDelegateListQuery() {
        DocProjectController controller = new DocProjectController(projectApplicationService, projectQueryApplicationService);
        DocProjectBo bo = new DocProjectBo();
        PageQuery pageQuery = new PageQuery(10, 1);
        TableDataInfo<DocProjectVo> expected = TableDataInfo.build(List.of(new DocProjectVo()));
        when(projectQueryApplicationService.list(bo, pageQuery)).thenReturn(expected);

        TableDataInfo<DocProjectVo> result = controller.list(bo, pageQuery);

        assertEquals(expected, result);
    }

    @Test
    void shouldWrapProjectInfoWithOkResponse() {
        DocProjectController controller = new DocProjectController(projectApplicationService, projectQueryApplicationService);
        DocProjectVo project = new DocProjectVo();
        project.setId(99L);
        when(projectQueryApplicationService.getById(99L)).thenReturn(project);

        R<DocProjectVo> result = controller.getInfo(99L);

        assertEquals(R.SUCCESS, result.getCode());
        assertEquals(project, result.getData());
    }

    @Test
    void shouldCreateProjectAndReturnId() {
        DocProjectController controller = new DocProjectController(projectApplicationService, projectQueryApplicationService);
        DocProjectBo bo = new DocProjectBo();
        bo.setName("新项目");
        when(projectApplicationService.create(bo)).thenReturn(123L);

        R<Long> result = controller.add(bo);

        assertEquals(R.SUCCESS, result.getCode());
        assertEquals(123L, result.getData());
        verify(projectApplicationService).create(bo);
    }

    @Test
    void shouldRejectEditWhenIdIsMissing() {
        DocProjectController controller = new DocProjectController(projectApplicationService, projectQueryApplicationService);
        DocProjectBo bo = new DocProjectBo();
        bo.setName("待修改项目");

        R<Void> result = controller.edit(bo);

        assertEquals(R.FAIL, result.getCode());
        assertEquals("项目ID不能为空", result.getMsg());
        assertNull(result.getData());
        verify(projectApplicationService, never()).update(bo);
    }

    @Test
    void shouldUpdateProjectWhenIdExists() {
        DocProjectController controller = new DocProjectController(projectApplicationService, projectQueryApplicationService);
        DocProjectBo bo = new DocProjectBo();
        bo.setId(7L);

        R<Void> result = controller.edit(bo);

        assertEquals(R.SUCCESS, result.getCode());
        verify(projectApplicationService).update(bo);
    }

    @Test
    void shouldDeleteProjects() {
        DocProjectController controller = new DocProjectController(projectApplicationService, projectQueryApplicationService);
        List<Long> ids = List.of(1L, 2L);

        R<Void> result = controller.remove(ids);

        assertEquals(R.SUCCESS, result.getCode());
        verify(projectApplicationService).delete(ids);
    }
}
