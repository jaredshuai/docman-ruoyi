package org.dromara.docman.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.docman.application.port.out.SystemMessagePort;
import org.dromara.docman.domain.bo.DocNodeDeadlineBo;
import org.dromara.docman.domain.entity.DocNodeDeadline;
import org.dromara.docman.domain.enums.DocProjectAction;
import org.dromara.docman.domain.vo.DocNodeDeadlineVo;
import org.dromara.docman.domain.vo.DocProjectVo;
import org.dromara.docman.mapper.DocNodeDeadlineMapper;
import org.dromara.docman.service.IDocProjectAccessService;
import org.dromara.docman.service.IDocProjectService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("dev")
@Tag("prod")
@Tag("local")
class DocNodeDeadlineServiceImplTest {

    @Mock
    private DocNodeDeadlineMapper nodeDeadlineMapper;

    @Mock
    private IDocProjectAccessService projectAccessService;

    @Mock
    private IDocProjectService projectService;

    @Mock
    private SystemMessagePort systemMessagePort;

    @InjectMocks
    private DocNodeDeadlineServiceImpl service;

    @Test
    void shouldSkipCreateWhenDurationNotPositive() {
        service.createDeadline(1L, "node-a", 2L, 0);

        verify(nodeDeadlineMapper, never()).selectOne(any(LambdaQueryWrapper.class));
        verify(nodeDeadlineMapper, never()).insert(any(DocNodeDeadline.class));
    }

    @Test
    void shouldUpdateExistingDeadlineAndResetReminderState() {
        DocNodeDeadline existing = new DocNodeDeadline();
        existing.setId(10L);
        existing.setReminderCount(3);
        existing.setLastRemindedAt(LocalDateTime.now());

        when(nodeDeadlineMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(existing);

        service.createDeadline(1L, "node-a", 2L, 5);

        ArgumentCaptor<DocNodeDeadline> captor = ArgumentCaptor.forClass(DocNodeDeadline.class);
        verify(nodeDeadlineMapper).updateById(captor.capture());
        DocNodeDeadline updated = captor.getValue();
        assertEquals(2L, updated.getProjectId());
        assertEquals(5, updated.getDurationDays());
        assertEquals(0, updated.getReminderCount());
        assertNull(updated.getLastRemindedAt());
        verify(nodeDeadlineMapper, never()).insert(any(DocNodeDeadline.class));
    }

    @Test
    void shouldInsertNewDeadlineWhenNoExistingRecord() {
        when(nodeDeadlineMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        service.createDeadline(8L, "demo-node", 3L, 4);

        ArgumentCaptor<DocNodeDeadline> captor = ArgumentCaptor.forClass(DocNodeDeadline.class);
        verify(nodeDeadlineMapper).insert(captor.capture());
        DocNodeDeadline created = captor.getValue();
        assertEquals(8L, created.getProcessInstanceId());
        assertEquals("demo-node", created.getNodeCode());
        assertEquals(3L, created.getProjectId());
        assertEquals(4, created.getDurationDays());
        assertEquals(0, created.getReminderCount());
        assertNull(created.getLastRemindedAt());
        assertNotNull(created.getDeadline());
    }

    @Test
    void shouldRejectUpdateWhenDeadlineAndDurationAreBothNull() {
        DocNodeDeadline existing = new DocNodeDeadline();
        existing.setId(1L);
        existing.setProjectId(9L);
        when(nodeDeadlineMapper.selectById(1L)).thenReturn(existing);

        DocNodeDeadlineBo bo = new DocNodeDeadlineBo();
        bo.setId(1L);

        ServiceException ex = assertThrows(ServiceException.class, () -> service.updateDeadline(bo));
        assertEquals("截止日期和时限天数不能同时为空", ex.getMessage());
        verify(projectAccessService).assertAction(9L, DocProjectAction.EDIT_PROJECT);
        verify(nodeDeadlineMapper, never()).updateById(any(DocNodeDeadline.class));
    }

    @Test
    void shouldRejectUpdateWhenDurationIsNotPositive() {
        DocNodeDeadline existing = new DocNodeDeadline();
        existing.setId(1L);
        existing.setProjectId(9L);
        when(nodeDeadlineMapper.selectById(1L)).thenReturn(existing);

        DocNodeDeadlineBo bo = new DocNodeDeadlineBo();
        bo.setId(1L);
        bo.setDurationDays(0);

        ServiceException ex = assertThrows(ServiceException.class, () -> service.updateDeadline(bo));
        assertEquals("时限天数必须大于 0", ex.getMessage());
        verify(nodeDeadlineMapper, never()).updateById(any(DocNodeDeadline.class));
    }

    @Test
    void shouldOnlyUpdateProvidedFieldsAndResetReminderWhenDeadlineChanges() {
        DocNodeDeadline existing = new DocNodeDeadline();
        existing.setId(1L);
        existing.setProjectId(5L);
        existing.setDurationDays(6);
        existing.setReminderCount(2);
        existing.setLastRemindedAt(LocalDateTime.now());
        when(nodeDeadlineMapper.selectById(1L)).thenReturn(existing);

        DocNodeDeadlineBo bo = new DocNodeDeadlineBo();
        bo.setId(1L);
        bo.setDeadline(LocalDate.of(2026, 3, 31));

        service.updateDeadline(bo);

        ArgumentCaptor<DocNodeDeadline> captor = ArgumentCaptor.forClass(DocNodeDeadline.class);
        verify(nodeDeadlineMapper).updateById(captor.capture());
        DocNodeDeadline updated = captor.getValue();
        assertEquals(LocalDate.of(2026, 3, 31), updated.getDeadline());
        assertEquals(6, updated.getDurationDays());
        assertEquals(0, updated.getReminderCount());
        assertNull(updated.getLastRemindedAt());
    }

    @Test
    void shouldSendReminderAndUpdateRecord() {
        DocNodeDeadlineVo vo = new DocNodeDeadlineVo();
        vo.setId(100L);
        vo.setProjectId(200L);
        vo.setNodeCode("demo-node");
        vo.setDeadline(LocalDate.of(2026, 3, 25));
        when(nodeDeadlineMapper.selectApproachingDeadlines(any(LocalDate.class), anyInt())).thenReturn(List.of(vo));

        DocProjectVo project = new DocProjectVo();
        project.setId(200L);
        project.setName("演示项目");
        project.setOwnerId(1L);
        when(projectService.queryById(200L)).thenReturn(project);

        DocNodeDeadline record = new DocNodeDeadline();
        record.setId(100L);
        record.setReminderCount(1);
        when(nodeDeadlineMapper.selectById(100L)).thenReturn(record);

        int reminded = service.sendApproachingDeadlineReminders(3, 2);

        assertEquals(1, reminded);
        verify(systemMessagePort).publishToUsers(List.of(1L), "项目【演示项目】节点【demo-node】文档截止日期为 2026-03-25，请及时处理");
        verify(nodeDeadlineMapper).updateById(record);
        assertEquals(2, record.getReminderCount());
        assertNotNull(record.getLastRemindedAt());
    }
}
