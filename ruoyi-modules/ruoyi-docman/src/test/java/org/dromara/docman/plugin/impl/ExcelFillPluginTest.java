package org.dromara.docman.plugin.impl;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.dromara.docman.application.port.out.DocumentStoragePort;
import org.dromara.docman.context.NodeContextReader;
import org.dromara.docman.domain.entity.DocNodeContext;
import org.dromara.docman.plugin.PluginContext;
import org.dromara.docman.plugin.PluginResult;
import org.dromara.docman.plugin.PluginType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Direct unit tests for ExcelFillPlugin without Spring context.
 */
@Tag("local")
class ExcelFillPluginTest {

    private ExcelFillPlugin plugin;
    private MockDocumentStoragePort mockStoragePort;

    @BeforeEach
    void setUp() {
        mockStoragePort = new MockDocumentStoragePort();
        plugin = new ExcelFillPlugin(mockStoragePort);
    }

    @Nested
    @DisplayName("missing config branch")
    class MissingConfig {

        @Test
        @DisplayName("should fail when pluginConfig is null")
        void shouldFailWhenConfigIsNull() {
            PluginContext context = PluginContext.builder()
                .pluginConfig(null)
                .build();

            PluginResult result = plugin.execute(context);

            assertFalse(result.isSuccess());
            assertEquals("缺少插件配置", result.getErrorMessage());
        }

        @Test
        @DisplayName("should fail when pluginConfig is empty")
        void shouldFailWhenConfigIsEmpty() {
            PluginContext context = PluginContext.builder()
                .pluginConfig(Collections.emptyMap())
                .build();

            PluginResult result = plugin.execute(context);

            assertFalse(result.isSuccess());
            assertEquals("缺少插件配置", result.getErrorMessage());
        }

        @Test
        @DisplayName("should fail when templatePath is missing")
        void shouldFailWhenTemplatePathMissing() {
            Map<String, Object> config = new HashMap<>();
            config.put("fieldMapping", Map.of("name", "A1"));

            PluginContext context = PluginContext.builder()
                .pluginConfig(config)
                .build();

            PluginResult result = plugin.execute(context);

            assertFalse(result.isSuccess());
            assertEquals("缺少必要配置: templatePath 或 fieldMapping", result.getErrorMessage());
        }

        @Test
        @DisplayName("should fail when fieldMapping is missing")
        void shouldFailWhenFieldMappingMissing() {
            Map<String, Object> config = new HashMap<>();
            config.put("templatePath", "/templates/test.xlsx");

            PluginContext context = PluginContext.builder()
                .pluginConfig(config)
                .build();

            PluginResult result = plugin.execute(context);

            assertFalse(result.isSuccess());
            assertEquals("缺少必要配置: templatePath 或 fieldMapping", result.getErrorMessage());
        }

        @Test
        @DisplayName("should fail when fieldMapping is empty")
        void shouldFailWhenFieldMappingEmpty() {
            Map<String, Object> config = new HashMap<>();
            config.put("templatePath", "/templates/test.xlsx");
            config.put("fieldMapping", Collections.emptyMap());

            PluginContext context = PluginContext.builder()
                .pluginConfig(config)
                .build();

            PluginResult result = plugin.execute(context);

            assertFalse(result.isSuccess());
            assertEquals("缺少必要配置: templatePath 或 fieldMapping", result.getErrorMessage());
        }
    }

    @Nested
    @DisplayName("sheet resolution failure branch")
    class SheetResolutionFailure {

        @Test
        @DisplayName("should fail when sheetName does not exist")
        void shouldFailWhenSheetNameNotFound() {
            Map<String, Object> config = createValidConfig();
            config.put("sheetName", "NonExistentSheet");

            PluginContext context = createValidContext(config);

            PluginResult result = plugin.execute(context);

            assertFalse(result.isSuccess());
            assertEquals("模板工作表不存在: NonExistentSheet", result.getErrorMessage());
        }

        @Test
        @DisplayName("should fail when sheetIndex is out of bounds")
        void shouldFailWhenSheetIndexOutOfBounds() {
            Map<String, Object> config = createValidConfig();
            config.put("sheetIndex", 99);

            PluginContext context = createValidContext(config);

            PluginResult result = plugin.execute(context);

            assertFalse(result.isSuccess());
            assertEquals("模板工作表不存在: 99", result.getErrorMessage());
        }

        @Test
        @DisplayName("should fail when sheetIndex is negative")
        void shouldFailWhenSheetIndexNegative() {
            Map<String, Object> config = createValidConfig();
            config.put("sheetIndex", -1);

            PluginContext context = createValidContext(config);

            PluginResult result = plugin.execute(context);

            assertFalse(result.isSuccess());
            assertEquals("模板工作表不存在: -1", result.getErrorMessage());
        }
    }

    @Nested
    @DisplayName("successful execution path")
    class SuccessfulExecution {

        @Test
        @DisplayName("should succeed with valid config and default sheet index")
        void shouldSucceedWithValidConfig() {
            Map<String, Object> config = createValidConfig();

            PluginContext context = createValidContext(config);

            PluginResult result = plugin.execute(context);

            assertTrue(result.isSuccess());
            assertNotNull(result.getGeneratedFiles());
            assertEquals(1, result.getGeneratedFiles().size());

            PluginResult.GeneratedFile file = result.getGeneratedFiles().get(0);
            assertNotNull(file.getFileName());
            assertTrue(file.getFileName().endsWith(".xlsx"));
            assertNotNull(file.getNasPath());
            assertEquals(1L, file.getOssId());
        }

        @Test
        @DisplayName("should succeed with valid sheetName")
        void shouldSucceedWithValidSheetName() {
            Map<String, Object> config = createValidConfig();
            config.put("sheetName", "Sheet1");

            PluginContext context = createValidContext(config);

            PluginResult result = plugin.execute(context);

            assertTrue(result.isSuccess());
            assertEquals(1, result.getGeneratedFiles().size());
        }

        @Test
        @DisplayName("should use outputFileName from config when provided")
        void shouldUseOutputFileNameFromConfig() {
            Map<String, Object> config = createValidConfig();
            config.put("outputFileName", "custom-output.xlsx");

            PluginContext context = createValidContext(config);

            PluginResult result = plugin.execute(context);

            assertTrue(result.isSuccess());
            PluginResult.GeneratedFile file = result.getGeneratedFiles().get(0);
            assertTrue(file.getFileName().contains("custom-output"));
        }

        @Test
        @DisplayName("should fallback to template extension when output file name has no extension")
        void shouldFallbackToTemplateExtensionWhenOutputFileNameHasNoExtension() {
            Map<String, Object> config = createValidConfig();
            config.put("templatePath", "/templates/template.xls");
            config.put("outputFileName", "custom-output");

            PluginContext context = createValidContext(config);

            PluginResult result = plugin.execute(context);

            assertTrue(result.isSuccess());
            PluginResult.GeneratedFile file = result.getGeneratedFiles().get(0);
            assertTrue(file.getFileName().endsWith(".xls"));
            assertEquals("application/vnd.ms-excel", mockStoragePort.getStoredContentType());
        }

        @Test
        @DisplayName("should skip null field values without error")
        void shouldSkipNullFieldValues() {
            Map<String, Object> config = createValidConfig();
            // fieldMapping includes a field that doesn't exist in context
            config.put("fieldMapping", Map.of(
                "existingField", "A1",
                "nonExistingField", "B1"
            ));

            DocNodeContext nodeContext = DocNodeContext.create(1L, "node1", 1L);
            nodeContext.setNodeVariables(Map.of("existingField", "test-value"));
            NodeContextReader reader = new NodeContextReader(List.of(nodeContext));

            PluginContext context = PluginContext.builder()
                .pluginConfig(config)
                .nasBasePath("/projects/1")
                .contextReader(reader)
                .build();

            PluginResult result = plugin.execute(context);

            assertTrue(result.isSuccess());
        }

        @Test
        @DisplayName("should normalize field mapping and write mapped cell")
        void shouldNormalizeFieldMappingAndWriteMappedCell() throws Exception {
            Map<String, Object> config = new HashMap<>();
            config.put("templatePath", "/templates/test.xlsx");
            config.put("fieldMapping", Map.of(
                " testField ", " A1 ",
                "   ", "B1",
                "ignoredField", "   "
            ));

            PluginContext context = createValidContext(config);

            PluginResult result = plugin.execute(context);

            assertTrue(result.isSuccess());
            assertEquals("test-value", readCellText(mockStoragePort.getStoredContent(), 0, 0));
            assertNull(readCellText(mockStoragePort.getStoredContent(), 0, 1));
        }

        @Test
        @DisplayName("should write number, boolean and date values to workbook")
        void shouldWriteNumberBooleanAndDateValuesToWorkbook() throws Exception {
            Map<String, Object> config = new HashMap<>();
            config.put("templatePath", "/templates/test.xlsx");
            config.put("fieldMapping", Map.of(
                "amount", "A1",
                "enabled", "B1",
                "dueDate", "C1",
                "updatedAt", "D1"
            ));

            DocNodeContext nodeContext = DocNodeContext.create(1L, "node1", 1L);
            nodeContext.setNodeVariables(Map.of(
                "amount", 12,
                "enabled", true,
                "dueDate", LocalDate.of(2026, 3, 23),
                "updatedAt", LocalDateTime.of(2026, 3, 23, 10, 0)
            ));
            NodeContextReader reader = new NodeContextReader(List.of(nodeContext));

            PluginContext context = PluginContext.builder()
                .pluginConfig(config)
                .nasBasePath("/projects/1")
                .contextReader(reader)
                .build();

            PluginResult result = plugin.execute(context);

            assertTrue(result.isSuccess());
            assertEquals(12.0d, readNumericCell(mockStoragePort.getStoredContent(), 0, 0));
            assertTrue(readBooleanCell(mockStoragePort.getStoredContent(), 0, 1));
            assertInstanceOf(Date.class, readDateCell(mockStoragePort.getStoredContent(), 0, 2));
            assertInstanceOf(Date.class, readDateCell(mockStoragePort.getStoredContent(), 0, 3));
        }
    }

    @Nested
    @DisplayName("plugin metadata")
    class PluginMetadata {

        @Test
        @DisplayName("should return correct plugin id")
        void shouldReturnCorrectPluginId() {
            assertEquals("excel-fill", plugin.getPluginId());
        }

        @Test
        @DisplayName("should return correct plugin name")
        void shouldReturnCorrectPluginName() {
            assertEquals("Excel填充插件", plugin.getPluginName());
        }

        @Test
        @DisplayName("should return correct plugin type")
        void shouldReturnCorrectPluginType() {
            assertEquals(PluginType.EXCEL_FILL, plugin.getPluginType());
        }

        @Test
        @DisplayName("should expose expected field definitions")
        void shouldExposeExpectedFieldDefinitions() {
            assertEquals(1, plugin.getInputFields().size());
            assertEquals("*", plugin.getInputFields().get(0).getName());
            assertEquals("dynamic", plugin.getInputFields().get(0).getType());
            assertTrue(plugin.getOutputFields().isEmpty());
        }
    }

    // Helper methods

    private Map<String, Object> createValidConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("templatePath", "/templates/test.xlsx");
        config.put("fieldMapping", Map.of("testField", "A1"));
        return config;
    }

    private PluginContext createValidContext(Map<String, Object> config) {
        DocNodeContext nodeContext = DocNodeContext.create(1L, "node1", 1L);
        nodeContext.setNodeVariables(Map.of("testField", "test-value"));
        NodeContextReader reader = new NodeContextReader(List.of(nodeContext));

        return PluginContext.builder()
            .pluginConfig(config)
            .nasBasePath("/projects/1")
            .contextReader(reader)
            .build();
    }

    private String readCellText(byte[] workbookBytes, int rowIndex, int columnIndex) throws Exception {
        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(workbookBytes))) {
            Row row = workbook.getSheetAt(0).getRow(rowIndex);
            return row == null || row.getCell(columnIndex) == null ? null : row.getCell(columnIndex).toString();
        }
    }

    private double readNumericCell(byte[] workbookBytes, int rowIndex, int columnIndex) throws Exception {
        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(workbookBytes))) {
            return workbook.getSheetAt(0).getRow(rowIndex).getCell(columnIndex).getNumericCellValue();
        }
    }

    private boolean readBooleanCell(byte[] workbookBytes, int rowIndex, int columnIndex) throws Exception {
        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(workbookBytes))) {
            return workbook.getSheetAt(0).getRow(rowIndex).getCell(columnIndex).getBooleanCellValue();
        }
    }

    private Date readDateCell(byte[] workbookBytes, int rowIndex, int columnIndex) throws Exception {
        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(workbookBytes))) {
            return workbook.getSheetAt(0).getRow(rowIndex).getCell(columnIndex).getDateCellValue();
        }
    }

    /**
     * Mock implementation of DocumentStoragePort for testing.
     */
    private static class MockDocumentStoragePort implements DocumentStoragePort {

        private byte[] storedContent;
        private String storedContentType;

        @Override
        public boolean ensureDirectory(String path) {
            return true;
        }

        @Override
        public byte[] load(String path) {
            // Return a minimal valid Excel workbook
            try (Workbook workbook = new XSSFWorkbook();
                 ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
                Sheet sheet = workbook.createSheet("Sheet1");
                sheet.createRow(0).createCell(0); // Create cell A1
                workbook.write(bos);
                return bos.toByteArray();
            } catch (Exception e) {
                throw new RuntimeException("Failed to create mock Excel", e);
            }
        }

        @Override
        public StoredDocument store(String path, byte[] content, String fileName, String contentType) {
            this.storedContent = content;
            this.storedContentType = contentType;
            return new StoredDocument(path, fileName, 1L);
        }

        byte[] getStoredContent() {
            return storedContent;
        }

        String getStoredContentType() {
            return storedContentType;
        }
    }
}
