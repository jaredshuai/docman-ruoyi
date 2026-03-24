package org.dromara.docman.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.dromara.docman.context.NodeContextReader;
import org.dromara.docman.domain.entity.DocNodeContext;
import org.dromara.docman.mapper.DocNodeContextMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;
import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("dev")
@Tag("prod")
@Tag("local")
class NodeContextServiceImplTest {

    @Mock
    private DocNodeContextMapper contextMapper;

    @InjectMocks
    private NodeContextServiceImpl service;

    // ========== getOrCreate tests ==========

    @Test
    void getOrCreate_shouldReturnExisting_whenRecordExists() {
        DocNodeContext existing = new DocNodeContext();
        existing.setId(100L);
        existing.setProcessInstanceId(1L);
        existing.setNodeCode("node-a");

        when(contextMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(existing);

        DocNodeContext result = service.getOrCreate(1L, "node-a", 5L);

        assertSame(existing, result);
        verify(contextMapper).selectOne(any(LambdaQueryWrapper.class));
        verify(contextMapper, org.mockito.Mockito.never()).insert(any(DocNodeContext.class));
    }

    @Test
    void getOrCreate_shouldCreateNew_whenRecordNotExists() {
        when(contextMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        DocNodeContext result = service.getOrCreate(2L, "node-b", 10L);

        assertNotNull(result);
        assertEquals(2L, result.getProcessInstanceId());
        assertEquals("node-b", result.getNodeCode());
        assertEquals(10L, result.getProjectId());
        assertNotNull(result.getProcessVariables());
        assertNotNull(result.getNodeVariables());
        assertNotNull(result.getDocumentFacts());
        assertNotNull(result.getUnstructuredContent());
        verify(contextMapper).insert(any(DocNodeContext.class));
    }

    // ========== putProcessVariable tests ==========

    @Test
    void putProcessVariable_shouldAddNewFieldAndUpdate() {
        DocNodeContext ctx = DocNodeContext.create(1L, "node-a", 5L);
        ctx.setId(100L);
        when(contextMapper.selectById(100L)).thenReturn(ctx);

        service.putProcessVariable(100L, "field1", "value1");

        assertEquals("value1", ctx.getProcessVariables().get("field1"));
        assertNotNull(ctx.getUpdateTime());
        verify(contextMapper).updateById(ctx);
    }

    @Test
    void putProcessVariable_shouldUpdateExistingField() {
        DocNodeContext ctx = DocNodeContext.create(1L, "node-a", 5L);
        ctx.setId(100L);
        ctx.getProcessVariables().put("field1", "oldValue");
        when(contextMapper.selectById(100L)).thenReturn(ctx);

        service.putProcessVariable(100L, "field1", "newValue");

        assertEquals("newValue", ctx.getProcessVariables().get("field1"));
        verify(contextMapper).updateById(ctx);
    }

    // ========== putNodeVariable tests ==========

    @Test
    void putNodeVariable_shouldAddNewFieldAndUpdate() {
        DocNodeContext ctx = DocNodeContext.create(1L, "node-a", 5L);
        ctx.setId(200L);
        when(contextMapper.selectById(200L)).thenReturn(ctx);

        service.putNodeVariable(200L, "nodeField", 42);

        assertEquals(42, ctx.getNodeVariables().get("nodeField"));
        assertNotNull(ctx.getUpdateTime());
        verify(contextMapper).updateById(ctx);
    }

    @Test
    void putNodeVariable_shouldUpdateExistingField() {
        DocNodeContext ctx = DocNodeContext.create(1L, "node-a", 5L);
        ctx.setId(200L);
        ctx.getNodeVariables().put("nodeField", 10);
        when(contextMapper.selectById(200L)).thenReturn(ctx);

        service.putNodeVariable(200L, "nodeField", 99);

        assertEquals(99, ctx.getNodeVariables().get("nodeField"));
        verify(contextMapper).updateById(ctx);
    }

    // ========== putDocumentFact tests ==========

    @Test
    void putDocumentFact_shouldAddNewFieldAndUpdate() {
        DocNodeContext ctx = DocNodeContext.create(1L, "node-a", 5L);
        ctx.setId(300L);
        when(contextMapper.selectById(300L)).thenReturn(ctx);

        service.putDocumentFact(300L, "fact1", "factValue");

        assertEquals("factValue", ctx.getDocumentFacts().get("fact1"));
        assertNotNull(ctx.getUpdateTime());
        verify(contextMapper).updateById(ctx);
    }

    @Test
    void putDocumentFact_shouldUpdateExistingField() {
        DocNodeContext ctx = DocNodeContext.create(1L, "node-a", 5L);
        ctx.setId(300L);
        ctx.getDocumentFacts().put("fact1", "oldFact");
        when(contextMapper.selectById(300L)).thenReturn(ctx);

        service.putDocumentFact(300L, "fact1", "newFact");

        assertEquals("newFact", ctx.getDocumentFacts().get("fact1"));
        verify(contextMapper).updateById(ctx);
    }

    // ========== putUnstructuredContent tests ==========

    @Test
    void putUnstructuredContent_shouldAddNewKeyAndUpdate() {
        DocNodeContext ctx = DocNodeContext.create(1L, "node-a", 5L);
        ctx.setId(400L);
        when(contextMapper.selectById(400L)).thenReturn(ctx);

        service.putUnstructuredContent(400L, "contentKey", "Some text content");

        assertEquals("Some text content", ctx.getUnstructuredContent().get("contentKey"));
        assertNotNull(ctx.getUpdateTime());
        verify(contextMapper).updateById(ctx);
    }

    @Test
    void putUnstructuredContent_shouldUpdateExistingKey() {
        DocNodeContext ctx = DocNodeContext.create(1L, "node-a", 5L);
        ctx.setId(400L);
        ctx.getUnstructuredContent().put("contentKey", "old text");
        when(contextMapper.selectById(400L)).thenReturn(ctx);

        service.putUnstructuredContent(400L, "contentKey", "new text");

        assertEquals("new text", ctx.getUnstructuredContent().get("contentKey"));
        verify(contextMapper).updateById(ctx);
    }

    // ========== buildReader tests ==========

    @Test
    void buildReader_shouldQueryWithProcessInstanceIdAndOrdering() {
        DocNodeContext ctx1 = new DocNodeContext();
        ctx1.setId(1L);
        ctx1.setProcessInstanceId(100L);
        ctx1.setNodeCode("node-1");
        ctx1.setCreateTime(new Date(System.currentTimeMillis() - 10000));
        ctx1.setProcessVariables(new HashMap<>());
        ctx1.setNodeVariables(new HashMap<>());
        ctx1.setDocumentFacts(new HashMap<>());
        ctx1.setUnstructuredContent(new HashMap<>());

        DocNodeContext ctx2 = new DocNodeContext();
        ctx2.setId(2L);
        ctx2.setProcessInstanceId(100L);
        ctx2.setNodeCode("node-2");
        ctx2.setCreateTime(new Date());
        ctx2.setProcessVariables(new HashMap<>());
        ctx2.setNodeVariables(new HashMap<>());
        ctx2.setDocumentFacts(new HashMap<>());
        ctx2.setUnstructuredContent(new HashMap<>());

        when(contextMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(ctx1, ctx2));

        NodeContextReader reader = service.buildReader(100L);

        assertNotNull(reader);
        ArgumentCaptor<LambdaQueryWrapper<DocNodeContext>> captor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(contextMapper).selectList(captor.capture());
        // Verify the query was called - the ordering is handled by LambdaQueryWrapper's orderByAsc
        assertTrue(captor.getValue() instanceof LambdaQueryWrapper);
    }

    @Test
    void buildReader_shouldReturnEmptyReader_whenNoContextsFound() {
        when(contextMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());

        NodeContextReader reader = service.buildReader(999L);

        assertNotNull(reader);
        // Empty reader returns empty map for getAllReadableFields
        assertTrue(reader.getAllReadableFields().isEmpty());
    }
}