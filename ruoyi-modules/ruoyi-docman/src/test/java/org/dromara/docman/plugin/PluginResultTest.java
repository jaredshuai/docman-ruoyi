package org.dromara.docman.plugin;

import org.dromara.docman.plugin.PluginResult.GeneratedFile;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for PluginResult factory helpers and GeneratedFile.
 */
@Tag("local")
class PluginResultTest {

    @Nested
    @DisplayName("ok() factory methods")
    class OkFactory {

        @Test
        @DisplayName("ok() should create success result without files")
        void okWithoutFiles_shouldCreateSuccessResult() {
            PluginResult result = PluginResult.ok();

            assertTrue(result.isSuccess());
            assertNull(result.getErrorMessage());
            assertNull(result.getGeneratedFiles());
        }

        @Test
        @DisplayName("ok(files) should create success result with files")
        void okWithFiles_shouldCreateSuccessResultWithFiles() {
            List<GeneratedFile> files = List.of(
                GeneratedFile.builder().fileName("test.xlsx").nasPath("/path/test.xlsx").build()
            );

            PluginResult result = PluginResult.ok(files);

            assertTrue(result.isSuccess());
            assertNull(result.getErrorMessage());
            assertNotNull(result.getGeneratedFiles());
            assertEquals(1, result.getGeneratedFiles().size());
            assertEquals("test.xlsx", result.getGeneratedFiles().get(0).getFileName());
        }

        @Test
        @DisplayName("ok(emptyList) should create success result with empty list")
        void okWithEmptyList_shouldCreateSuccessResultWithEmptyList() {
            PluginResult result = PluginResult.ok(List.of());

            assertTrue(result.isSuccess());
            assertNotNull(result.getGeneratedFiles());
            assertTrue(result.getGeneratedFiles().isEmpty());
        }
    }

    @Nested
    @DisplayName("fail() factory method")
    class FailFactory {

        @Test
        @DisplayName("fail(message) should create failure result with message")
        void fail_shouldCreateFailureResult() {
            PluginResult result = PluginResult.fail("Execution failed");

            assertFalse(result.isSuccess());
            assertEquals("Execution failed", result.getErrorMessage());
            assertNull(result.getGeneratedFiles());
        }

        @Test
        @DisplayName("fail(null) should create failure result with null message")
        void failWithNullMessage_shouldCreateFailureResultWithNullMessage() {
            PluginResult result = PluginResult.fail(null);

            assertFalse(result.isSuccess());
            assertNull(result.getErrorMessage());
        }

        @Test
        @DisplayName("fail(empty) should create failure result with empty message")
        void failWithEmptyMessage_shouldCreateFailureResultWithEmptyMessage() {
            PluginResult result = PluginResult.fail("");

            assertFalse(result.isSuccess());
            assertEquals("", result.getErrorMessage());
        }
    }

    @Nested
    @DisplayName("GeneratedFile nested class")
    class GeneratedFileTest {

        @Test
        @DisplayName("should build GeneratedFile with all fields")
        void shouldBuildWithAllFields() {
            GeneratedFile file = GeneratedFile.builder()
                .fileName("report.xlsx")
                .nasPath("/docs/report.xlsx")
                .ossId(12345L)
                .build();

            assertEquals("report.xlsx", file.getFileName());
            assertEquals("/docs/report.xlsx", file.getNasPath());
            assertEquals(12345L, file.getOssId());
        }

        @Test
        @DisplayName("should build GeneratedFile with null fields")
        void shouldBuildWithNullFields() {
            GeneratedFile file = GeneratedFile.builder().build();

            assertNull(file.getFileName());
            assertNull(file.getNasPath());
            assertNull(file.getOssId());
        }
    }

    @Nested
    @DisplayName("Builder pattern")
    class BuilderPattern {

        @Test
        @DisplayName("should allow manual builder construction")
        void shouldAllowManualBuilderConstruction() {
            PluginResult result = PluginResult.builder()
                .success(true)
                .errorMessage(null)
                .generatedFiles(List.of())
                .build();

            assertTrue(result.isSuccess());
            assertNull(result.getErrorMessage());
            assertNotNull(result.getGeneratedFiles());
        }
    }
}