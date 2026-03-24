package org.dromara.docman.listener;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEvent;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for DocmanNodeFinishEvent constructor, getters, and inheritance.
 */
@Tag("local")
class DocmanNodeFinishEventTest {

    @Nested
    @DisplayName("Constructor")
    class Constructor {

        @Test
        @DisplayName("should create event with all fields")
        void shouldCreateEventWithAllFields() {
            Object source = new Object();

            DocmanNodeFinishEvent event = new DocmanNodeFinishEvent(
                source, 100L, "NODE_001", "extension_data"
            );

            assertEquals(100L, event.getProcessInstanceId());
            assertEquals("NODE_001", event.getNodeCode());
            assertEquals("extension_data", event.getNodeExt());
            assertSame(source, event.getSource());
        }

        @Test
        @DisplayName("should accept null nodeExt")
        void shouldAcceptNullNodeExt() {
            DocmanNodeFinishEvent event = new DocmanNodeFinishEvent(
                this, 1L, "N1", null
            );

            assertNull(event.getNodeExt());
        }

        @Test
        @DisplayName("should accept null nodeCode")
        void shouldAcceptNullNodeCode() {
            DocmanNodeFinishEvent event = new DocmanNodeFinishEvent(
                this, 1L, null, "ext"
            );

            assertNull(event.getNodeCode());
        }
    }

    @Nested
    @DisplayName("Getters")
    class Getters {

        @Test
        @DisplayName("getProcessInstanceId should return correct value")
        void getProcessInstanceId_shouldReturnCorrectValue() {
            DocmanNodeFinishEvent event = new DocmanNodeFinishEvent(
                this, 999L, "CODE", "ext"
            );

            assertEquals(999L, event.getProcessInstanceId());
        }

        @Test
        @DisplayName("getNodeCode should return correct value")
        void getNodeCode_shouldReturnCorrectValue() {
            DocmanNodeFinishEvent event = new DocmanNodeFinishEvent(
                this, 1L, "MY_NODE", "ext"
            );

            assertEquals("MY_NODE", event.getNodeCode());
        }

        @Test
        @DisplayName("getNodeExt should return correct value")
        void getNodeExt_shouldReturnCorrectValue() {
            DocmanNodeFinishEvent event = new DocmanNodeFinishEvent(
                this, 1L, "N1", "my_extension_data"
            );

            assertEquals("my_extension_data", event.getNodeExt());
        }
    }

    @Nested
    @DisplayName("Inheritance")
    class Inheritance {

        @Test
        @DisplayName("should extend ApplicationEvent")
        void shouldExtendApplicationEvent() {
            DocmanNodeFinishEvent event = new DocmanNodeFinishEvent(this, 1L, "N1", "ext");

            assertTrue(event instanceof ApplicationEvent);
        }

        @Test
        @DisplayName("should inherit timestamp from ApplicationEvent")
        void shouldInheritTimestamp() {
            long before = System.currentTimeMillis();

            DocmanNodeFinishEvent event = new DocmanNodeFinishEvent(this, 1L, "N1", "ext");

            long after = System.currentTimeMillis();

            assertTrue(event.getTimestamp() >= before);
            assertTrue(event.getTimestamp() <= after);
        }

        @Test
        @DisplayName("getSource should return the source object")
        void getSource_shouldReturnSourceObject() {
            Object source = new Object();

            DocmanNodeFinishEvent event = new DocmanNodeFinishEvent(source, 1L, "N1", "ext");

            assertSame(source, event.getSource());
        }
    }

    @Nested
    @DisplayName("Deprecation annotation")
    class DeprecationAnnotation {

        @Test
        @DisplayName("should be marked as deprecated for removal")
        void shouldBeDeprecatedForRemoval() {
            assertTrue(DocmanNodeFinishEvent.class.isAnnotationPresent(Deprecated.class));

            Deprecated deprecated = DocmanNodeFinishEvent.class.getAnnotation(Deprecated.class);
            assertTrue(deprecated.forRemoval());
        }
    }
}