package org.dromara.docman.domain.enums;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 枚举类单元测试。
 */
@Tag("local")
class DocEnumsTest {

    @Nested
    @DisplayName("DocArchiveStatus")
    class DocArchiveStatusTest {

        @Test
        @DisplayName("of() 正常查找")
        void shouldFindStatusByCode() {
            assertEquals(DocArchiveStatus.REQUESTED, DocArchiveStatus.of("requested"));
            assertEquals(DocArchiveStatus.GENERATING, DocArchiveStatus.of("generating"));
            assertEquals(DocArchiveStatus.COMPLETED, DocArchiveStatus.of("completed"));
            assertEquals(DocArchiveStatus.FAILED, DocArchiveStatus.of("failed"));
        }

        @Test
        @DisplayName("of() 未知 code 返回默认值 COMPLETED")
        void shouldReturnDefaultForUnknownCode() {
            assertEquals(DocArchiveStatus.COMPLETED, DocArchiveStatus.of("unknown"));
            assertEquals(DocArchiveStatus.COMPLETED, DocArchiveStatus.of(""));
            assertEquals(DocArchiveStatus.COMPLETED, DocArchiveStatus.of(null));
        }

        @Test
        @DisplayName("code 和 label 属性正确")
        void shouldHaveCorrectCodeAndLabel() {
            assertEquals("requested", DocArchiveStatus.REQUESTED.getCode());
            assertEquals("已申请", DocArchiveStatus.REQUESTED.getLabel());
        }
    }

    @Nested
    @DisplayName("DocDocumentStatus")
    class DocDocumentStatusTest {

        @Test
        @DisplayName("of() 正常查找")
        void shouldFindStatusByCode() {
            assertEquals(DocDocumentStatus.PENDING, DocDocumentStatus.of("pending"));
            assertEquals(DocDocumentStatus.RUNNING, DocDocumentStatus.of("running"));
            assertEquals(DocDocumentStatus.GENERATED, DocDocumentStatus.of("generated"));
            assertEquals(DocDocumentStatus.FAILED, DocDocumentStatus.of("failed"));
            assertEquals(DocDocumentStatus.ARCHIVED, DocDocumentStatus.of("archived"));
            assertEquals(DocDocumentStatus.OBSOLETE, DocDocumentStatus.of("obsolete"));
        }

        @Test
        @DisplayName("of() 未知 code 抛出异常")
        void shouldThrowForUnknownCode() {
            assertThrows(IllegalArgumentException.class, () -> DocDocumentStatus.of("unknown"));
            assertThrows(IllegalArgumentException.class, () -> DocDocumentStatus.of(""));
        }

        @Test
        @DisplayName("code 和 label 属性正确")
        void shouldHaveCorrectCodeAndLabel() {
            assertEquals("pending", DocDocumentStatus.PENDING.getCode());
            assertEquals("待生成", DocDocumentStatus.PENDING.getLabel());
        }
    }

    @Nested
    @DisplayName("DocProjectStatus")
    class DocProjectStatusTest {

        @Test
        @DisplayName("of() 正常查找")
        void shouldFindStatusByCode() {
            assertEquals(DocProjectStatus.ACTIVE, DocProjectStatus.of("active"));
            assertEquals(DocProjectStatus.ARCHIVED, DocProjectStatus.of("archived"));
        }

        @Test
        @DisplayName("of() 未知 code 抛出异常")
        void shouldThrowForUnknownCode() {
            assertThrows(IllegalArgumentException.class, () -> DocProjectStatus.of("unknown"));
            assertThrows(IllegalArgumentException.class, () -> DocProjectStatus.of(""));
        }

        @Test
        @DisplayName("code 和 label 属性正确")
        void shouldHaveCorrectCodeAndLabel() {
            assertEquals("active", DocProjectStatus.ACTIVE.getCode());
            assertEquals("进行中", DocProjectStatus.ACTIVE.getLabel());
        }
    }

    @Nested
    @DisplayName("DocPluginExecutionStatus")
    class DocPluginExecutionStatusTest {

        @Test
        @DisplayName("of() 正常查找")
        void shouldFindStatusByCode() {
            assertEquals(DocPluginExecutionStatus.SUCCESS, DocPluginExecutionStatus.of("success"));
            assertEquals(DocPluginExecutionStatus.FAILED, DocPluginExecutionStatus.of("failed"));
        }

        @Test
        @DisplayName("of() 未知 code 抛出异常")
        void shouldThrowForUnknownCode() {
            assertThrows(IllegalArgumentException.class, () -> DocPluginExecutionStatus.of("unknown"));
            assertThrows(IllegalArgumentException.class, () -> DocPluginExecutionStatus.of(""));
        }

        @Test
        @DisplayName("code 和 label 属性正确")
        void shouldHaveCorrectCodeAndLabel() {
            assertEquals("success", DocPluginExecutionStatus.SUCCESS.getCode());
            assertEquals("成功", DocPluginExecutionStatus.SUCCESS.getLabel());
        }
    }

    @Nested
    @DisplayName("DocProcessConfigStatus")
    class DocProcessConfigStatusTest {

        @Test
        @DisplayName("of() 正常查找")
        void shouldFindStatusByCode() {
            assertEquals(DocProcessConfigStatus.PENDING, DocProcessConfigStatus.of("pending"));
            assertEquals(DocProcessConfigStatus.RUNNING, DocProcessConfigStatus.of("running"));
            assertEquals(DocProcessConfigStatus.COMPLETED, DocProcessConfigStatus.of("completed"));
        }

        @Test
        @DisplayName("of() 未知 code 抛出异常")
        void shouldThrowForUnknownCode() {
            assertThrows(IllegalArgumentException.class, () -> DocProcessConfigStatus.of("unknown"));
            assertThrows(IllegalArgumentException.class, () -> DocProcessConfigStatus.of(""));
        }
    }
}