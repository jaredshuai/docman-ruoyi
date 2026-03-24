package org.dromara.docman.application.assembler;

import org.dromara.docman.domain.vo.DocPluginInfoVo;
import org.dromara.docman.plugin.DocumentPlugin;
import org.dromara.docman.plugin.FieldDef;
import org.dromara.docman.plugin.PluginContext;
import org.dromara.docman.plugin.PluginResult;
import org.dromara.docman.plugin.PluginType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Direct unit tests for DocPluginAssembler without Spring context.
 */
@Tag("local")
class DocPluginAssemblerTest {

    private final DocPluginAssembler assembler = new DocPluginAssembler();

    private static DocumentPlugin createPlugin(String id, String name, PluginType type,
                                                List<FieldDef> inputFields, List<FieldDef> outputFields) {
        return new DocumentPlugin() {
            @Override public String getPluginId() { return id; }
            @Override public String getPluginName() { return name; }
            @Override public PluginType getPluginType() { return type; }
            @Override public List<FieldDef> getInputFields() { return inputFields; }
            @Override public List<FieldDef> getOutputFields() { return outputFields; }
            @Override public PluginResult execute(PluginContext ctx) { return null; }
        };
    }

    @Nested
    @DisplayName("toInfoVo() field mapping")
    class ToInfoVo {

        @Test
        @DisplayName("should map all fields from DocumentPlugin to DocPluginInfoVo")
        void shouldMapAllFields() {
            List<FieldDef> inputFields = List.of(
                FieldDef.builder().name("input1").type("string").required(true).description("Input field 1").build()
            );
            List<FieldDef> outputFields = List.of(
                FieldDef.builder().name("output1").type("number").required(false).description("Output field 1").build()
            );
            DocumentPlugin plugin = createPlugin("test-plugin-id", "Test Plugin", PluginType.EXCEL_FILL, inputFields, outputFields);

            DocPluginInfoVo vo = assembler.toInfoVo(plugin);

            assertEquals("test-plugin-id", vo.getPluginId());
            assertEquals("Test Plugin", vo.getPluginName());
            assertEquals("excel_fill", vo.getPluginType());
            assertEquals(1, vo.getInputFields().size());
            assertEquals("input1", vo.getInputFields().get(0).getName());
            assertEquals(1, vo.getOutputFields().size());
            assertEquals("output1", vo.getOutputFields().get(0).getName());
        }

        @Test
        @DisplayName("should map pluginType code correctly for EXCEL_FILL")
        void shouldMapExcelFillType() {
            DocumentPlugin plugin = createPlugin("id", "name", PluginType.EXCEL_FILL, List.of(), List.of());

            DocPluginInfoVo vo = assembler.toInfoVo(plugin);

            assertEquals("excel_fill", vo.getPluginType());
        }

        @Test
        @DisplayName("should map pluginType code correctly for DATA_EXTRACT")
        void shouldMapDataExtractType() {
            DocumentPlugin plugin = createPlugin("id", "name", PluginType.DATA_EXTRACT, List.of(), List.of());

            DocPluginInfoVo vo = assembler.toInfoVo(plugin);

            assertEquals("data_extract", vo.getPluginType());
        }

        @Test
        @DisplayName("should map pluginType code correctly for AI_GENERATE")
        void shouldMapAiGenerateType() {
            DocumentPlugin plugin = createPlugin("id", "name", PluginType.AI_GENERATE, List.of(), List.of());

            DocPluginInfoVo vo = assembler.toInfoVo(plugin);

            assertEquals("ai_generate", vo.getPluginType());
        }

        @Test
        @DisplayName("should handle empty input and output fields")
        void shouldHandleEmptyFields() {
            DocumentPlugin plugin = createPlugin("empty-plugin", "Empty Plugin", PluginType.DATA_EXTRACT, List.of(), List.of());

            DocPluginInfoVo vo = assembler.toInfoVo(plugin);

            assertTrue(vo.getInputFields().isEmpty());
            assertTrue(vo.getOutputFields().isEmpty());
        }

        @Test
        @DisplayName("should handle multiple input and output fields")
        void shouldHandleMultipleFields() {
            List<FieldDef> inputFields = List.of(
                FieldDef.builder().name("in1").type("string").build(),
                FieldDef.builder().name("in2").type("number").build(),
                FieldDef.builder().name("in3").type("boolean").build()
            );
            List<FieldDef> outputFields = List.of(
                FieldDef.builder().name("out1").type("object").build(),
                FieldDef.builder().name("out2").type("array").build()
            );
            DocumentPlugin plugin = createPlugin("multi-plugin", "Multi Plugin", PluginType.AI_GENERATE, inputFields, outputFields);

            DocPluginInfoVo vo = assembler.toInfoVo(plugin);

            assertEquals(3, vo.getInputFields().size());
            assertEquals(2, vo.getOutputFields().size());
            assertEquals("in1", vo.getInputFields().get(0).getName());
            assertEquals("in2", vo.getInputFields().get(1).getName());
            assertEquals("in3", vo.getInputFields().get(2).getName());
            assertEquals("out1", vo.getOutputFields().get(0).getName());
            assertEquals("out2", vo.getOutputFields().get(1).getName());
        }

        @Test
        @DisplayName("should preserve FieldDef properties in mapped fields")
        void shouldPreserveFieldDefProperties() {
            List<FieldDef> inputFields = List.of(
                FieldDef.builder().name("field1").type("string").required(true).description("Required string field").build()
            );
            List<FieldDef> outputFields = List.of(
                FieldDef.builder().name("result").type("object").required(false).description("Result object").build()
            );
            DocumentPlugin plugin = createPlugin("props-plugin", "Props Plugin", PluginType.EXCEL_FILL, inputFields, outputFields);

            DocPluginInfoVo vo = assembler.toInfoVo(plugin);

            FieldDef inputField = vo.getInputFields().get(0);
            assertEquals("field1", inputField.getName());
            assertEquals("string", inputField.getType());
            assertTrue(inputField.isRequired());
            assertEquals("Required string field", inputField.getDescription());

            FieldDef outputField = vo.getOutputFields().get(0);
            assertEquals("result", outputField.getName());
            assertEquals("object", outputField.getType());
            assertFalse(outputField.isRequired());
            assertEquals("Result object", outputField.getDescription());
        }
    }
}