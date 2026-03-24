package org.dromara.docman.domain.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for DocNodeContext.create() factory method and defaults.
 */
@Tag("local")
class DocNodeContextTest {

    @Nested
    @DisplayName("create() factory method")
    class CreateFactory {

        @Test
        @DisplayName("should create context with required fields")
        void shouldCreateWithRequiredFields() {
            DocNodeContext ctx = DocNodeContext.create(100L, "NODE_001", 200L);

            assertEquals(100L, ctx.getProcessInstanceId());
            assertEquals("NODE_001", ctx.getNodeCode());
            assertEquals(200L, ctx.getProjectId());
        }

        @Test
        @DisplayName("should initialize all map fields as empty HashMaps")
        void shouldInitializeEmptyMaps() {
            DocNodeContext ctx = DocNodeContext.create(1L, "N1", 1L);

            assertNotNull(ctx.getProcessVariables());
            assertNotNull(ctx.getNodeVariables());
            assertNotNull(ctx.getDocumentFacts());
            assertNotNull(ctx.getUnstructuredContent());

            assertTrue(ctx.getProcessVariables().isEmpty());
            assertTrue(ctx.getNodeVariables().isEmpty());
            assertTrue(ctx.getDocumentFacts().isEmpty());
            assertTrue(ctx.getUnstructuredContent().isEmpty());
        }

        @Test
        @DisplayName("should set createTime and updateTime to current time")
        void shouldSetTimestamps() {
            Date before = new Date();

            DocNodeContext ctx = DocNodeContext.create(1L, "N1", 1L);

            Date after = new Date();

            assertNotNull(ctx.getCreateTime());
            assertNotNull(ctx.getUpdateTime());

            assertTrue(ctx.getCreateTime().getTime() >= before.getTime());
            assertTrue(ctx.getCreateTime().getTime() <= after.getTime());
            assertTrue(ctx.getUpdateTime().getTime() >= before.getTime());
            assertTrue(ctx.getUpdateTime().getTime() <= after.getTime());
        }

        @Test
        @DisplayName("should leave id null (not persisted)")
        void shouldLeaveIdNull() {
            DocNodeContext ctx = DocNodeContext.create(1L, "N1", 1L);

            assertNull(ctx.getId());
        }
    }

    @Nested
    @DisplayName("Default constructor and setters")
    class DefaultConstructor {

        @Test
        @DisplayName("should create instance with null fields")
        void shouldCreateWithNullFields() {
            DocNodeContext ctx = new DocNodeContext();

            assertNull(ctx.getId());
            assertNull(ctx.getProcessInstanceId());
            assertNull(ctx.getNodeCode());
            assertNull(ctx.getProjectId());
            assertNull(ctx.getProcessVariables());
            assertNull(ctx.getNodeVariables());
            assertNull(ctx.getDocumentFacts());
            assertNull(ctx.getUnstructuredContent());
            assertNull(ctx.getCreateTime());
            assertNull(ctx.getUpdateTime());
        }

        @Test
        @DisplayName("should allow setting fields after construction")
        void shouldAllowSettingFields() {
            DocNodeContext ctx = new DocNodeContext();
            Date now = new Date();

            ctx.setId(999L);
            ctx.setProcessInstanceId(10L);
            ctx.setNodeCode("CODE");
            ctx.setProjectId(20L);
            ctx.setCreateTime(now);
            ctx.setUpdateTime(now);

            assertEquals(999L, ctx.getId());
            assertEquals(10L, ctx.getProcessInstanceId());
            assertEquals("CODE", ctx.getNodeCode());
            assertEquals(20L, ctx.getProjectId());
            assertEquals(now, ctx.getCreateTime());
            assertEquals(now, ctx.getUpdateTime());
        }
    }

    @Nested
    @DisplayName("Map mutability")
    class MapMutability {

        @Test
        @DisplayName("processVariables should be mutable HashMap")
        void processVariablesShouldBeMutable() {
            DocNodeContext ctx = DocNodeContext.create(1L, "N1", 1L);

            ctx.getProcessVariables().put("key", "value");

            assertEquals("value", ctx.getProcessVariables().get("key"));
        }

        @Test
        @DisplayName("nodeVariables should be mutable HashMap")
        void nodeVariablesShouldBeMutable() {
            DocNodeContext ctx = DocNodeContext.create(1L, "N1", 1L);

            ctx.getNodeVariables().put("var", 123);

            assertEquals(123, ctx.getNodeVariables().get("var"));
        }

        @Test
        @DisplayName("documentFacts should be mutable HashMap")
        void documentFactsShouldBeMutable() {
            DocNodeContext ctx = DocNodeContext.create(1L, "N1", 1L);

            ctx.getDocumentFacts().put("fact", Map.of("data", "value"));

            assertNotNull(ctx.getDocumentFacts().get("fact"));
        }

        @Test
        @DisplayName("unstructuredContent should be mutable HashMap")
        void unstructuredContentShouldBeMutable() {
            DocNodeContext ctx = DocNodeContext.create(1L, "N1", 1L);

            ctx.getUnstructuredContent().put("section", "content");

            assertEquals("content", ctx.getUnstructuredContent().get("section"));
        }
    }

    @Nested
    @DisplayName("Serializable implementation")
    class SerializableImplementation {

        @Test
        @DisplayName("should implement Serializable")
        void shouldImplementSerializable() {
            DocNodeContext ctx = new DocNodeContext();

            assertTrue(ctx instanceof java.io.Serializable);
        }

        @Test
        @DisplayName("should have serialVersionUID field")
        void shouldHaveSerialVersionUID() throws Exception {
            var field = DocNodeContext.class.getDeclaredField("serialVersionUID");
            field.setAccessible(true);

            assertTrue(java.lang.reflect.Modifier.isStatic(field.getModifiers()));
            assertTrue(java.lang.reflect.Modifier.isFinal(field.getModifiers()));
            assertEquals(1L, field.get(null));
        }
    }
}