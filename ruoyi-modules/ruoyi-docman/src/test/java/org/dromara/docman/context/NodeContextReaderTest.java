package org.dromara.docman.context;

import org.dromara.docman.domain.entity.DocNodeContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@Tag("dev")
@Tag("prod")
@Tag("local")
class NodeContextReaderTest {

    private List<DocNodeContext> contexts;
    private DocNodeContext ctx1;
    private DocNodeContext ctx2;
    private DocNodeContext ctx3;

    @BeforeEach
    void setUp() {
        contexts = new ArrayList<>();

        ctx1 = new DocNodeContext();
        ctx1.setNodeCode("node1");
        ctx1.setProcessVariables(new HashMap<>(Map.of("processVar1", "value1", "sharedVar", "processFromCtx1")));
        ctx1.setNodeVariables(new HashMap<>(Map.of("nodeField1", "nodeValue1", "sharedNodeVar", "nodeFromCtx1")));
        ctx1.setDocumentFacts(new HashMap<>(Map.of("fact1", "factValue1", "sharedFact", "factFromCtx1")));
        ctx1.setUnstructuredContent(new HashMap<>(Map.of("content1", "unstructured1")));

        ctx2 = new DocNodeContext();
        ctx2.setNodeCode("node2");
        ctx2.setProcessVariables(new HashMap<>(Map.of("processVar2", "value2", "sharedVar", "processFromCtx2")));
        ctx2.setNodeVariables(new HashMap<>(Map.of("nodeField2", "nodeValue2")));
        ctx2.setDocumentFacts(new HashMap<>(Map.of("fact2", "factValue2", "sharedFact", "factFromCtx2")));
        ctx2.setUnstructuredContent(new HashMap<>(Map.of("content2", "unstructured2")));

        ctx3 = new DocNodeContext();
        ctx3.setNodeCode("node3");
        ctx3.setProcessVariables(null);
        ctx3.setNodeVariables(null);
        ctx3.setDocumentFacts(null);
        ctx3.setUnstructuredContent(null);

        contexts.add(ctx1);
        contexts.add(ctx2);
        contexts.add(ctx3);
    }

    // ========== Structured Field Lookup Tests ==========

    @Test
    void getStructuredField_shouldReturnFieldValue_whenNodeExists() {
        NodeContextReader reader = new NodeContextReader(contexts);

        Object result = reader.getStructuredField("node1", "nodeField1");

        assertEquals("nodeValue1", result);
    }

    @Test
    void getStructuredField_shouldReturnNull_whenNodeNotExists() {
        NodeContextReader reader = new NodeContextReader(contexts);

        Object result = reader.getStructuredField("nonExistentNode", "nodeField1");

        assertNull(result);
    }

    @Test
    void getStructuredField_shouldReturnNull_whenFieldNotExists() {
        NodeContextReader reader = new NodeContextReader(contexts);

        Object result = reader.getStructuredField("node1", "nonExistentField");

        assertNull(result);
    }

    @Test
    void getStructuredField_shouldThrowNPE_whenNodeVariablesNull() {
        // Note: Production code does not handle null nodeVariables - throws NPE
        NodeContextReader reader = new NodeContextReader(contexts);

        assertThrows(NullPointerException.class, () ->
            reader.getStructuredField("node3", "anyField"));
    }

    // ========== Process Variable Lookup Tests ==========

    @Test
    void getProcessVariable_shouldReturnFirstMatch_whenExists() {
        NodeContextReader reader = new NodeContextReader(contexts);

        Object result = reader.getProcessVariable("processVar1");

        assertEquals("value1", result);
    }

    @Test
    void getProcessVariable_shouldReturnNull_whenNotExists() {
        NodeContextReader reader = new NodeContextReader(contexts);

        Object result = reader.getProcessVariable("nonExistentVar");

        assertNull(result);
    }

    @Test
    void getProcessVariable_shouldReturnNull_whenAllProcessVariablesNull() {
        List<DocNodeContext> nullOnlyContexts = new ArrayList<>();
        DocNodeContext nullCtx = new DocNodeContext();
        nullCtx.setNodeCode("nullNode");
        nullCtx.setProcessVariables(null);
        nullOnlyContexts.add(nullCtx);

        NodeContextReader reader = new NodeContextReader(nullOnlyContexts);

        Object result = reader.getProcessVariable("anyVar");

        assertNull(result);
    }

    @Test
    void getProcessVariable_shouldSkipNullProcessVariables() {
        NodeContextReader reader = new NodeContextReader(contexts);

        // ctx3 has null processVariables, but ctx1 has the variable
        Object result = reader.getProcessVariable("processVar1");

        assertEquals("value1", result);
    }

    // ========== Document Fact Last-Write-Wins Tests ==========

    @Test
    void getDocumentFact_shouldReturnLastValue_whenMultipleMatches() {
        NodeContextReader reader = new NodeContextReader(contexts);

        // sharedFact exists in both ctx1 and ctx2, should return ctx2's value (last-write-wins)
        Object result = reader.getDocumentFact("sharedFact");

        assertEquals("factFromCtx2", result);
    }

    @Test
    void getDocumentFact_shouldReturnSingleValue_whenOnlyOneMatch() {
        NodeContextReader reader = new NodeContextReader(contexts);

        Object result = reader.getDocumentFact("fact1");

        assertEquals("factValue1", result);
    }

    @Test
    void getDocumentFact_shouldReturnNull_whenNotExists() {
        NodeContextReader reader = new NodeContextReader(contexts);

        Object result = reader.getDocumentFact("nonExistentFact");

        assertNull(result);
    }

    @Test
    void getDocumentFact_shouldSkipNullDocumentFacts() {
        NodeContextReader reader = new NodeContextReader(contexts);

        // ctx3 has null documentFacts, but should still find fact1 from ctx1
        Object result = reader.getDocumentFact("fact1");

        assertEquals("factValue1", result);
    }

    @Test
    void getDocumentFact_shouldReturnLastNonNullValue() {
        List<DocNodeContext> orderedContexts = new ArrayList<>();
        DocNodeContext first = new DocNodeContext();
        first.setNodeCode("first");
        first.setDocumentFacts(new HashMap<>(Map.of("key", "firstValue")));
        DocNodeContext second = new DocNodeContext();
        second.setNodeCode("second");
        second.setDocumentFacts(new HashMap<>(Map.of("key", "secondValue")));
        DocNodeContext third = new DocNodeContext();
        third.setNodeCode("third");
        third.setDocumentFacts(null);
        orderedContexts.add(first);
        orderedContexts.add(second);
        orderedContexts.add(third);

        NodeContextReader reader = new NodeContextReader(orderedContexts);

        Object result = reader.getDocumentFact("key");

        assertEquals("secondValue", result);
    }

    // ========== Unstructured Content Lookup Tests ==========

    @Test
    void getUnstructuredContent_shouldReturnValue_whenNodeAndKeyExist() {
        NodeContextReader reader = new NodeContextReader(contexts);

        String result = reader.getUnstructuredContent("node1", "content1");

        assertEquals("unstructured1", result);
    }

    @Test
    void getUnstructuredContent_shouldReturnNull_whenNodeNotExists() {
        NodeContextReader reader = new NodeContextReader(contexts);

        String result = reader.getUnstructuredContent("nonExistentNode", "content1");

        assertNull(result);
    }

    @Test
    void getUnstructuredContent_shouldReturnNull_whenKeyNotExists() {
        NodeContextReader reader = new NodeContextReader(contexts);

        String result = reader.getUnstructuredContent("node1", "nonExistentKey");

        assertNull(result);
    }

    @Test
    void getUnstructuredContent_shouldThrowNPE_whenUnstructuredContentNull() {
        // Note: Production code does not handle null unstructuredContent - throws NPE
        NodeContextReader reader = new NodeContextReader(contexts);

        assertThrows(NullPointerException.class, () ->
            reader.getUnstructuredContent("node3", "anyKey"));
    }

    // ========== Merged Readable Fields Tests ==========

    @Test
    void getAllReadableFields_shouldMergeAllSources() {
        NodeContextReader reader = new NodeContextReader(contexts);

        Map<String, Object> result = reader.getAllReadableFields();

        assertTrue(result.containsKey("processVar1"));
        assertTrue(result.containsKey("processVar2"));
        assertTrue(result.containsKey("nodeField1"));
        assertTrue(result.containsKey("nodeField2"));
        assertTrue(result.containsKey("fact1"));
        assertTrue(result.containsKey("fact2"));
    }

    @Test
    void getAllReadableFields_shouldApplyCorrectMergeOrder() {
        // Merge order: processVariables -> documentFacts -> nodeVariables (last wins)
        NodeContextReader reader = new NodeContextReader(contexts);

        Map<String, Object> result = reader.getAllReadableFields();

        // sharedVar exists in processVariables of both ctx1 and ctx2
        // ctx2's processVariables comes after ctx1's, so processFromCtx2 wins
        assertEquals("processFromCtx2", result.get("sharedVar"));
    }

    @Test
    void getAllReadableFields_shouldHandleNullMaps() {
        NodeContextReader reader = new NodeContextReader(contexts);

        // Should not throw NPE for ctx3 which has all null maps
        Map<String, Object> result = reader.getAllReadableFields();

        assertNotNull(result);
    }

    @Test
    void getAllReadableFields_shouldReturnEmptyMap_whenAllEmpty() {
        List<DocNodeContext> emptyContexts = new ArrayList<>();
        DocNodeContext emptyCtx = new DocNodeContext();
        emptyCtx.setNodeCode("empty");
        emptyCtx.setProcessVariables(new HashMap<>());
        emptyCtx.setNodeVariables(new HashMap<>());
        emptyCtx.setDocumentFacts(new HashMap<>());
        emptyContexts.add(emptyCtx);

        NodeContextReader reader = new NodeContextReader(emptyContexts);

        Map<String, Object> result = reader.getAllReadableFields();

        assertTrue(result.isEmpty());
    }

    // ========== Merged Document Facts Tests ==========

    @Test
    void getAllDocumentFacts_shouldMergeAllFacts() {
        NodeContextReader reader = new NodeContextReader(contexts);

        Map<String, Object> result = reader.getAllDocumentFacts();

        assertEquals(3, result.size());
        assertEquals("factValue1", result.get("fact1"));
        assertEquals("factValue2", result.get("fact2"));
        assertEquals("factFromCtx2", result.get("sharedFact")); // last-write-wins
    }

    @Test
    void getAllDocumentFacts_shouldReturnEmptyMap_whenAllNull() {
        List<DocNodeContext> nullOnlyContexts = new ArrayList<>();
        DocNodeContext nullCtx = new DocNodeContext();
        nullCtx.setNodeCode("nullNode");
        nullCtx.setDocumentFacts(null);
        nullOnlyContexts.add(nullCtx);

        NodeContextReader reader = new NodeContextReader(nullOnlyContexts);

        Map<String, Object> result = reader.getAllDocumentFacts();

        assertTrue(result.isEmpty());
    }

    @Test
    void getAllDocumentFacts_shouldSkipNullMaps() {
        NodeContextReader reader = new NodeContextReader(contexts);

        // ctx3 has null documentFacts, should not affect result
        Map<String, Object> result = reader.getAllDocumentFacts();

        assertTrue(result.containsKey("fact1"));
        assertTrue(result.containsKey("fact2"));
    }

    // ========== Unmodifiable Maps Tests ==========

    @Test
    void getAllReadableFields_shouldReturnUnmodifiableMap() {
        NodeContextReader reader = new NodeContextReader(contexts);

        Map<String, Object> result = reader.getAllReadableFields();

        assertThrows(UnsupportedOperationException.class, () ->
            result.put("newKey", "newValue"));
    }

    @Test
    void getAllDocumentFacts_shouldReturnUnmodifiableMap() {
        NodeContextReader reader = new NodeContextReader(contexts);

        Map<String, Object> result = reader.getAllDocumentFacts();

        assertThrows(UnsupportedOperationException.class, () ->
            result.put("newKey", "newValue"));
    }

    @Test
    void getAllUnstructuredContent_shouldReturnUnmodifiableMap() {
        NodeContextReader reader = new NodeContextReader(contexts);

        Map<String, String> result = reader.getAllUnstructuredContent();

        assertThrows(UnsupportedOperationException.class, () ->
            result.put("newKey", "newValue"));
    }

    @Test
    void getAllUnstructuredContent_shouldMergeAllContent() {
        NodeContextReader reader = new NodeContextReader(contexts);

        Map<String, String> result = reader.getAllUnstructuredContent();

        assertEquals(2, result.size());
        assertEquals("unstructured1", result.get("content1"));
        assertEquals("unstructured2", result.get("content2"));
    }

    @Test
    void getAllUnstructuredContent_shouldHandleNullMaps() {
        NodeContextReader reader = new NodeContextReader(contexts);

        // ctx3 has null unstructuredContent, should not affect result
        Map<String, String> result = reader.getAllUnstructuredContent();

        assertNotNull(result);
        assertEquals(2, result.size());
    }

    @Test
    void getAllUnstructuredContent_shouldReturnEmptyMap_whenAllNull() {
        List<DocNodeContext> nullOnlyContexts = new ArrayList<>();
        DocNodeContext nullCtx = new DocNodeContext();
        nullCtx.setNodeCode("nullNode");
        nullCtx.setUnstructuredContent(null);
        nullOnlyContexts.add(nullCtx);

        NodeContextReader reader = new NodeContextReader(nullOnlyContexts);

        Map<String, String> result = reader.getAllUnstructuredContent();

        assertTrue(result.isEmpty());
    }
}