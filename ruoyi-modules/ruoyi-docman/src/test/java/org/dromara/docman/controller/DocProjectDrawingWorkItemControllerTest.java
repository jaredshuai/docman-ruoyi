package org.dromara.docman.controller;

import org.dromara.common.core.domain.R;
import org.dromara.docman.application.service.DocProjectDrawingWorkItemApplicationService;
import org.dromara.docman.application.service.DocProjectDrawingWorkItemQueryApplicationService;
import org.dromara.docman.domain.bo.DocProjectDrawingWorkItemBo;
import org.dromara.docman.domain.vo.DocProjectDrawingWorkItemVo;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("dev")
@Tag("prod")
@Tag("local")
class DocProjectDrawingWorkItemControllerTest {

    @Mock
    private DocProjectDrawingWorkItemQueryApplicationService queryApplicationService;

    @Mock
    private DocProjectDrawingWorkItemApplicationService applicationService;

    @Test
    void shouldListWorkItemsByProject() {
        DocProjectDrawingWorkItemController controller = new DocProjectDrawingWorkItemController(queryApplicationService, applicationService);
        DocProjectDrawingWorkItemVo vo = new DocProjectDrawingWorkItemVo();
        vo.setId(1L);
        when(queryApplicationService.listByProject(7L)).thenReturn(List.of(vo));

        R<List<DocProjectDrawingWorkItemVo>> result = controller.listByProject(7L);

        assertEquals(R.SUCCESS, result.getCode());
        assertEquals(1, result.getData().size());
        verify(queryApplicationService).listByProject(7L);
    }

    @Test
    void shouldSaveWorkItem() {
        DocProjectDrawingWorkItemController controller = new DocProjectDrawingWorkItemController(queryApplicationService, applicationService);
        DocProjectDrawingWorkItemBo bo = new DocProjectDrawingWorkItemBo();
        bo.setProjectId(7L);
        bo.setDrawingId(8L);
        when(applicationService.save(bo)).thenReturn(9L);

        R<Long> result = controller.save(bo);

        assertEquals(R.SUCCESS, result.getCode());
        assertEquals(9L, result.getData());
        verify(applicationService).save(bo);
    }
}
