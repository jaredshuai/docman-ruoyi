package org.dromara.docman.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.dromara.docman.domain.entity.DocProjectDrawing;
import org.dromara.docman.domain.entity.DocProjectDrawingWorkItem;
import org.dromara.docman.domain.enums.DocProjectAction;
import org.dromara.docman.domain.vo.DocProjectDrawingWorkItemVo;
import org.dromara.docman.mapper.DocProjectDrawingMapper;
import org.dromara.docman.mapper.DocProjectDrawingWorkItemMapper;
import org.dromara.docman.service.IDocProjectAccessService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("dev")
@Tag("prod")
@Tag("local")
class DocProjectDrawingWorkItemServiceImplTest {

    @Mock
    private DocProjectDrawingWorkItemMapper workItemMapper;

    @Mock
    private DocProjectDrawingMapper drawingMapper;

    @Mock
    private IDocProjectAccessService projectAccessService;

    @InjectMocks
    private DocProjectDrawingWorkItemServiceImpl service;

    @Test
    void shouldListWorkItemsByProject() {
        DocProjectDrawingWorkItem first = new DocProjectDrawingWorkItem();
        first.setId(1L);
        first.setDrawingId(10L);
        DocProjectDrawingWorkItem second = new DocProjectDrawingWorkItem();
        second.setId(2L);
        second.setDrawingId(11L);
        when(workItemMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(first, second));

        List<DocProjectDrawingWorkItemVo> result = service.listByProject(7L);

        assertEquals(2, result.size());
        verify(projectAccessService).assertAction(7L, DocProjectAction.VIEW_PROJECT);
        verify(workItemMapper).selectList(any(LambdaQueryWrapper.class));
    }

    @Test
    void shouldListWorkItemsByDrawing() {
        DocProjectDrawingWorkItem item = new DocProjectDrawingWorkItem();
        item.setId(3L);
        when(workItemMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(item));

        List<DocProjectDrawingWorkItemVo> result = service.listByDrawing(7L, 8L);

        assertEquals(1, result.size());
        verify(projectAccessService).assertAction(7L, DocProjectAction.VIEW_PROJECT);
        verify(workItemMapper).selectList(any(LambdaQueryWrapper.class));
        verify(drawingMapper, never()).selectById(any());
    }

    @Test
    void shouldSaveWorkItemUnderDrawing() {
        DocProjectDrawing drawing = new DocProjectDrawing();
        drawing.setId(8L);
        drawing.setProjectId(7L);
        when(drawingMapper.selectById(8L)).thenReturn(drawing);

        var bo = new org.dromara.docman.domain.bo.DocProjectDrawingWorkItemBo();
        bo.setProjectId(7L);
        bo.setDrawingId(8L);
        bo.setWorkItemName("杆路整治");

        service.save(bo);

        verify(projectAccessService).assertAction(7L, DocProjectAction.EDIT_PROJECT);
        verify(workItemMapper).insert(any(DocProjectDrawingWorkItem.class));
    }
}
