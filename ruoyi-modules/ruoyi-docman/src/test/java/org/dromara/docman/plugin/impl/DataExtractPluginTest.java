package org.dromara.docman.plugin.impl;

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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Direct unit tests for DataExtractPlugin without Spring context.
 */
@Tag("dev")
class DataExtractPluginTest {

    private DataExtractPlugin plugin;

    @BeforeEach
    void setUp() {
        plugin = new DataExtractPlugin();
    }

    @Nested
    @DisplayName("missing config branch")
    class MissingConfig {

        @Test
        @DisplayName("should fail when pluginConfig is null")
        void shouldFailWhenConfigIsNull() {
            PluginContext context = PluginContext.builder()
                .pluginConfig(null)
                .contextReader(createEmptyReader())
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
                .contextReader(createEmptyReader())
                .build();

            PluginResult result = plugin.execute(context);

            assertFalse(result.isSuccess());
            assertEquals("缺少插件配置", result.getErrorMessage());
        }

        @Test
        @DisplayName("should fail when extractRules is null")
        void shouldFailWhenExtractRulesIsNull() {
            Map<String, Object> config = new HashMap<>();
            config.put("extractRules", null);

            PluginContext context = PluginContext.builder()
                .pluginConfig(config)
                .contextReader(createEmptyReader())
                .build();

            PluginResult result = plugin.execute(context);

            assertFalse(result.isSuccess());
            assertEquals("缺少必要配置: extractRules", result.getErrorMessage());
        }

        @Test
        @DisplayName("should fail when extractRules is empty")
        void shouldFailWhenExtractRulesIsEmpty() {
            Map<String, Object> config = new HashMap<>();
            config.put("extractRules", Collections.emptyList());

            PluginContext context = PluginContext.builder()
                .pluginConfig(config)
                .contextReader(createEmptyReader())
                .build();

            PluginResult result = plugin.execute(context);

            assertFalse(result.isSuccess());
            assertEquals("缺少必要配置: extractRules", result.getErrorMessage());
        }
    }

    @Nested
    @DisplayName("copy mode basics")
    class CopyMode {

        @Test
        @DisplayName("should copy value from structured source")
        void shouldCopyFromStructuredSource() {
            Map<String, Object> config = new HashMap<>();
            config.put("extractRules", List.of(
                Map.of(
                    "target", "outputField",
                    "source", "inputField",
                    "sourceType", "structured"
                )
            ));

            DocNodeContext nodeContext = DocNodeContext.create(1L, "node1", 1L);
            nodeContext.setNodeVariables(Map.of("inputField", "copied-value"));
            NodeContextReader reader = new NodeContextReader(List.of(nodeContext));

            Map<String, Object> written = new HashMap<>();
            PluginContext context = PluginContext.builder()
                .pluginConfig(config)
                .contextReader(reader)
                .factWriter(written::put)
                .build();

            PluginResult result = plugin.execute(context);

            assertTrue(result.isSuccess());
            assertEquals("copied-value", written.get("outputField"));
        }

        @Test
        @DisplayName("should copy value from unstructured source")
        void shouldCopyFromUnstructuredSource() {
            Map<String, Object> config = new HashMap<>();
            config.put("extractRules", List.of(
                Map.of(
                    "target", "outputField",
                    "source", "contentKey",
                    "sourceType", "unstructured"
                )
            ));

            DocNodeContext nodeContext = DocNodeContext.create(1L, "node1", 1L);
            nodeContext.setUnstructuredContent(Map.of("contentKey", "unstructured-value"));
            NodeContextReader reader = new NodeContextReader(List.of(nodeContext));

            Map<String, Object> written = new HashMap<>();
            PluginContext context = PluginContext.builder()
                .pluginConfig(config)
                .contextReader(reader)
                .factWriter(written::put)
                .build();

            PluginResult result = plugin.execute(context);

            assertTrue(result.isSuccess());
            assertEquals("unstructured-value", written.get("outputField"));
        }

        @Test
        @DisplayName("should return null when source is missing")
        void shouldReturnNullWhenSourceMissing() {
            Map<String, Object> config = new HashMap<>();
            config.put("extractRules", List.of(
                Map.of(
                    "target", "outputField",
                    "source", "nonExistentField",
                    "sourceType", "structured"
                )
            ));

            DocNodeContext nodeContext = DocNodeContext.create(1L, "node1", 1L);
            nodeContext.setNodeVariables(Collections.emptyMap());
            NodeContextReader reader = new NodeContextReader(List.of(nodeContext));

            Map<String, Object> written = new HashMap<>();
            PluginContext context = PluginContext.builder()
                .pluginConfig(config)
                .contextReader(reader)
                .factWriter(written::put)
                .build();

            PluginResult result = plugin.execute(context);

            assertTrue(result.isSuccess());
            assertFalse(written.containsKey("outputField"));
        }

        @Test
        @DisplayName("should write to process scope when targetScope is process")
        void shouldWriteToProcessScope() {
            Map<String, Object> config = new HashMap<>();
            config.put("extractRules", List.of(
                Map.of(
                    "target", "outputField",
                    "source", "inputField",
                    "targetScope", "process"
                )
            ));

            DocNodeContext nodeContext = DocNodeContext.create(1L, "node1", 1L);
            nodeContext.setNodeVariables(Map.of("inputField", "process-value"));
            NodeContextReader reader = new NodeContextReader(List.of(nodeContext));

            Map<String, Object> processWritten = new HashMap<>();
            Map<String, Object> factWritten = new HashMap<>();
            PluginContext context = PluginContext.builder()
                .pluginConfig(config)
                .contextReader(reader)
                .processWriter(processWritten::put)
                .factWriter(factWritten::put)
                .build();

            PluginResult result = plugin.execute(context);

            assertTrue(result.isSuccess());
            assertEquals("process-value", processWritten.get("outputField"));
            assertFalse(factWritten.containsKey("outputField"));
        }

        @Test
        @DisplayName("should write to node scope when targetScope is node")
        void shouldWriteToNodeScope() {
            Map<String, Object> config = new HashMap<>();
            config.put("extractRules", List.of(
                Map.of(
                    "target", "outputField",
                    "source", "inputField",
                    "targetScope", "node"
                )
            ));

            DocNodeContext nodeContext = DocNodeContext.create(1L, "node1", 1L);
            nodeContext.setNodeVariables(Map.of("inputField", "node-value"));
            NodeContextReader reader = new NodeContextReader(List.of(nodeContext));

            Map<String, Object> nodeWritten = new HashMap<>();
            Map<String, Object> factWritten = new HashMap<>();
            PluginContext context = PluginContext.builder()
                .pluginConfig(config)
                .contextReader(reader)
                .nodeWriter(nodeWritten::put)
                .factWriter(factWritten::put)
                .build();

            PluginResult result = plugin.execute(context);

            assertTrue(result.isSuccess());
            assertEquals("node-value", nodeWritten.get("outputField"));
            assertFalse(factWritten.containsKey("outputField"));
        }

        @Test
        @DisplayName("should write to content scope when targetScope is content")
        void shouldWriteToContentScope() {
            Map<String, Object> config = new HashMap<>();
            config.put("extractRules", List.of(
                Map.of(
                    "target", "outputField",
                    "source", "inputField",
                    "targetScope", "content"
                )
            ));

            DocNodeContext nodeContext = DocNodeContext.create(1L, "node1", 1L);
            nodeContext.setNodeVariables(Map.of("inputField", "content-value"));
            NodeContextReader reader = new NodeContextReader(List.of(nodeContext));

            Map<String, String> contentWritten = new HashMap<>();
            PluginContext context = PluginContext.builder()
                .pluginConfig(config)
                .contextReader(reader)
                .contentWriter(contentWritten::put)
                .build();

            PluginResult result = plugin.execute(context);

            assertTrue(result.isSuccess());
            assertEquals("content-value", contentWritten.get("outputField"));
        }
    }

    @Nested
    @DisplayName("regex extraction")
    class RegexExtraction {

        @Test
        @DisplayName("should extract value using regex pattern")
        void shouldExtractUsingRegex() {
            Map<String, Object> config = new HashMap<>();
            config.put("extractRules", List.of(
                Map.of(
                    "target", "extractedValue",
                    "pattern", "value=([A-Z]+)",
                    "extractType", "regex"
                )
            ));

            DocNodeContext nodeContext = DocNodeContext.create(1L, "node1", 1L);
            nodeContext.setUnstructuredContent(Map.of("text", "prefix value=ABC suffix"));
            NodeContextReader reader = new NodeContextReader(List.of(nodeContext));

            Map<String, Object> written = new HashMap<>();
            PluginContext context = PluginContext.builder()
                .pluginConfig(config)
                .contextReader(reader)
                .factWriter(written::put)
                .build();

            PluginResult result = plugin.execute(context);

            assertTrue(result.isSuccess());
            assertEquals("ABC", written.get("extractedValue"));
        }

        @Test
        @DisplayName("should extract using named group")
        void shouldExtractUsingNamedGroup() {
            Map<String, Object> config = new HashMap<>();
            config.put("extractRules", List.of(
                Map.of(
                    "target", "extractedName",
                    "pattern", "name=(?<name>\\w+)",
                    "groupName", "name",
                    "extractType", "regex"
                )
            ));

            DocNodeContext nodeContext = DocNodeContext.create(1L, "node1", 1L);
            nodeContext.setUnstructuredContent(Map.of("text", "hello name=John world"));
            NodeContextReader reader = new NodeContextReader(List.of(nodeContext));

            Map<String, Object> written = new HashMap<>();
            PluginContext context = PluginContext.builder()
                .pluginConfig(config)
                .contextReader(reader)
                .factWriter(written::put)
                .build();

            PluginResult result = plugin.execute(context);

            assertTrue(result.isSuccess());
            assertEquals("John", written.get("extractedName"));
        }

        @Test
        @DisplayName("should extract using specific group index")
        void shouldExtractUsingGroupIndex() {
            Map<String, Object> config = new HashMap<>();
            config.put("extractRules", List.of(
                Map.of(
                    "target", "extractedCode",
                    "pattern", "(\\d{4})-(\\d{2})-(\\d{2})",
                    "group", 2,
                    "extractType", "regex"
                )
            ));

            DocNodeContext nodeContext = DocNodeContext.create(1L, "node1", 1L);
            nodeContext.setUnstructuredContent(Map.of("text", "date: 2024-03-15"));
            NodeContextReader reader = new NodeContextReader(List.of(nodeContext));

            Map<String, Object> written = new HashMap<>();
            PluginContext context = PluginContext.builder()
                .pluginConfig(config)
                .contextReader(reader)
                .factWriter(written::put)
                .build();

            PluginResult result = plugin.execute(context);

            assertTrue(result.isSuccess());
            assertEquals("03", written.get("extractedCode"));
        }

        @Test
        @DisplayName("should extract using matchIndex")
        void shouldExtractUsingMatchIndex() {
            Map<String, Object> config = new HashMap<>();
            config.put("extractRules", List.of(
                Map.of(
                    "target", "extractedItem",
                    "pattern", "item=(\\w+)",
                    "matchIndex", 1,
                    "extractType", "regex"
                )
            ));

            DocNodeContext nodeContext = DocNodeContext.create(1L, "node1", 1L);
            nodeContext.setUnstructuredContent(Map.of("text", "item=first item=second item=third"));
            NodeContextReader reader = new NodeContextReader(List.of(nodeContext));

            Map<String, Object> written = new HashMap<>();
            PluginContext context = PluginContext.builder()
                .pluginConfig(config)
                .contextReader(reader)
                .factWriter(written::put)
                .build();

            PluginResult result = plugin.execute(context);

            assertTrue(result.isSuccess());
            assertEquals("second", written.get("extractedItem"));
        }

        @Test
        @DisplayName("should extract using case insensitive flag")
        void shouldExtractUsingCaseInsensitiveFlag() {
            Map<String, Object> config = new HashMap<>();
            config.put("extractRules", List.of(
                Map.of(
                    "target", "extractedValue",
                    "pattern", "value=([a-z]+)",
                    "flags", "i",
                    "extractType", "regex"
                )
            ));

            DocNodeContext nodeContext = DocNodeContext.create(1L, "node1", 1L);
            nodeContext.setUnstructuredContent(Map.of("text", "VALUE=UPPERCASE"));
            NodeContextReader reader = new NodeContextReader(List.of(nodeContext));

            Map<String, Object> written = new HashMap<>();
            PluginContext context = PluginContext.builder()
                .pluginConfig(config)
                .contextReader(reader)
                .factWriter(written::put)
                .build();

            PluginResult result = plugin.execute(context);

            assertTrue(result.isSuccess());
            assertEquals("UPPERCASE", written.get("extractedValue"));
        }

        @Test
        @DisplayName("should fail with invalid regex pattern")
        void shouldFailWithInvalidRegex() {
            Map<String, Object> config = new HashMap<>();
            config.put("extractRules", List.of(
                Map.of(
                    "target", "extractedValue",
                    "pattern", "[invalid(regex",
                    "extractType", "regex"
                )
            ));

            DocNodeContext nodeContext = DocNodeContext.create(1L, "node1", 1L);
            nodeContext.setUnstructuredContent(Map.of("text", "some text"));
            NodeContextReader reader = new NodeContextReader(List.of(nodeContext));

            Map<String, Object> written = new HashMap<>();
            PluginContext context = PluginContext.builder()
                .pluginConfig(config)
                .contextReader(reader)
                .factWriter(written::put)
                .build();

            PluginResult result = plugin.execute(context);

            assertFalse(result.isSuccess());
            assertTrue(result.getErrorMessage().contains("regex 规则不合法"));
        }

        @Test
        @DisplayName("should fail when regex pattern is missing")
        void shouldFailWhenPatternMissing() {
            Map<String, Object> config = new HashMap<>();
            config.put("extractRules", List.of(
                Map.of(
                    "target", "extractedValue",
                    "extractType", "regex"
                )
            ));

            DocNodeContext nodeContext = DocNodeContext.create(1L, "node1", 1L);
            nodeContext.setUnstructuredContent(Map.of("text", "some text"));
            NodeContextReader reader = new NodeContextReader(List.of(nodeContext));

            Map<String, Object> written = new HashMap<>();
            PluginContext context = PluginContext.builder()
                .pluginConfig(config)
                .contextReader(reader)
                .factWriter(written::put)
                .build();

            PluginResult result = plugin.execute(context);

            assertFalse(result.isSuccess());
            assertTrue(result.getErrorMessage().contains("regex 规则缺少 pattern"));
        }

        @Test
        @DisplayName("should return null when regex does not match")
        void shouldReturnNullWhenNoMatch() {
            Map<String, Object> config = new HashMap<>();
            config.put("extractRules", List.of(
                Map.of(
                    "target", "extractedValue",
                    "pattern", "\\d{4}",
                    "extractType", "regex"
                )
            ));

            DocNodeContext nodeContext = DocNodeContext.create(1L, "node1", 1L);
            nodeContext.setUnstructuredContent(Map.of("text", "no digits here"));
            NodeContextReader reader = new NodeContextReader(List.of(nodeContext));

            Map<String, Object> written = new HashMap<>();
            PluginContext context = PluginContext.builder()
                .pluginConfig(config)
                .contextReader(reader)
                .factWriter(written::put)
                .build();

            PluginResult result = plugin.execute(context);

            assertTrue(result.isSuccess());
            assertFalse(written.containsKey("extractedValue"));
        }
    }

    @Nested
    @DisplayName("extraction mode inference")
    class ExtractionModeInference {

        @Test
        @DisplayName("should infer regex mode from regex alias")
        void shouldInferRegexModeFromRegexAlias() {
            Map<String, Object> config = new HashMap<>();
            config.put("extractRules", List.of(
                Map.of(
                    "target", "extractedValue",
                    "regex", "value=(\\w+)"
                )
            ));

            DocNodeContext nodeContext = DocNodeContext.create(1L, "node1", 1L);
            nodeContext.setUnstructuredContent(Map.of("text", "value=alias"));
            NodeContextReader reader = new NodeContextReader(List.of(nodeContext));

            Map<String, Object> written = new HashMap<>();
            PluginContext context = PluginContext.builder()
                .pluginConfig(config)
                .contextReader(reader)
                .factWriter(written::put)
                .build();

            PluginResult result = plugin.execute(context);

            assertTrue(result.isSuccess());
            assertEquals("alias", written.get("extractedValue"));
        }

        @Test
        @DisplayName("should infer xpath mode from expression alias")
        void shouldInferXpathModeFromExpressionAlias() {
            Map<String, Object> config = new HashMap<>();
            config.put("extractRules", List.of(
                Map.of(
                    "target", "extractedValue",
                    "expression", "//value/text()"
                )
            ));

            DocNodeContext nodeContext = DocNodeContext.create(1L, "node1", 1L);
            nodeContext.setUnstructuredContent(Map.of("xml", "<root><value>xpath</value></root>"));
            NodeContextReader reader = new NodeContextReader(List.of(nodeContext));

            Map<String, Object> written = new HashMap<>();
            PluginContext context = PluginContext.builder()
                .pluginConfig(config)
                .contextReader(reader)
                .factWriter(written::put)
                .build();

            PluginResult result = plugin.execute(context);

            assertTrue(result.isSuccess());
            assertEquals("xpath", written.get("extractedValue"));
        }

        @Test
        @DisplayName("should use merged unstructured content when source is blank")
        void shouldUseMergedUnstructuredContentWhenSourceBlank() {
            Map<String, Object> config = new HashMap<>();
            config.put("extractRules", List.of(
                Map.of(
                    "target", "extractedValue",
                    "pattern", "token=(\\w+)",
                    "extractType", "regex"
                )
            ));

            DocNodeContext nodeContext = DocNodeContext.create(1L, "node1", 1L);
            nodeContext.setUnstructuredContent(Map.of(
                "part1", "prefix",
                "part2", "token=merged"
            ));
            NodeContextReader reader = new NodeContextReader(List.of(nodeContext));

            Map<String, Object> written = new HashMap<>();
            PluginContext context = PluginContext.builder()
                .pluginConfig(config)
                .contextReader(reader)
                .factWriter(written::put)
                .build();

            PluginResult result = plugin.execute(context);

            assertTrue(result.isSuccess());
            assertEquals("merged", written.get("extractedValue"));
        }

        @Test
        @DisplayName("should fall back to structured source text when unstructured key is missing")
        void shouldFallbackToStructuredSourceTextWhenUnstructuredKeyMissing() {
            Map<String, Object> config = new HashMap<>();
            config.put("extractRules", List.of(
                Map.of(
                    "target", "extractedValue",
                    "source", "structuredField",
                    "pattern", "ID:(\\d+)",
                    "extractType", "regex"
                )
            ));

            DocNodeContext nodeContext = DocNodeContext.create(1L, "node1", 1L);
            nodeContext.setNodeVariables(Map.of("structuredField", "ID:42"));
            NodeContextReader reader = new NodeContextReader(List.of(nodeContext));

            Map<String, Object> written = new HashMap<>();
            PluginContext context = PluginContext.builder()
                .pluginConfig(config)
                .contextReader(reader)
                .factWriter(written::put)
                .build();

            PluginResult result = plugin.execute(context);

            assertTrue(result.isSuccess());
            assertEquals("42", written.get("extractedValue"));
        }

        @Test
        @DisplayName("should keep markers when include flags are enabled")
        void shouldKeepMarkersWhenIncludeFlagsEnabled() {
            Map<String, Object> config = new HashMap<>();
            config.put("extractRules", List.of(
                Map.of(
                    "target", "extractedValue",
                    "startMarker", "[",
                    "endMarker", "]",
                    "includeStart", true,
                    "includeEnd", true,
                    "extractType", "position"
                )
            ));

            DocNodeContext nodeContext = DocNodeContext.create(1L, "node1", 1L);
            nodeContext.setUnstructuredContent(Map.of("text", "prefix [VALUE] suffix"));
            NodeContextReader reader = new NodeContextReader(List.of(nodeContext));

            Map<String, Object> written = new HashMap<>();
            PluginContext context = PluginContext.builder()
                .pluginConfig(config)
                .contextReader(reader)
                .factWriter(written::put)
                .build();

            PluginResult result = plugin.execute(context);

            assertTrue(result.isSuccess());
            assertEquals("[VALUE]", written.get("extractedValue"));
        }
    }

    @Nested
    @DisplayName("value type conversion")
    class ValueTypeConversion {

        @Test
        @DisplayName("should convert to integer")
        void shouldConvertToInteger() {
            Map<String, Object> config = new HashMap<>();
            config.put("extractRules", List.of(
                Map.of(
                    "target", "intValue",
                    "fixedValue", "42",
                    "valueType", "int"
                )
            ));

            Map<String, Object> written = new HashMap<>();
            PluginContext context = PluginContext.builder()
                .pluginConfig(config)
                .contextReader(createEmptyReader())
                .factWriter(written::put)
                .build();

            PluginResult result = plugin.execute(context);

            assertTrue(result.isSuccess());
            assertEquals(42, written.get("intValue"));
            assertInstanceOf(Integer.class, written.get("intValue"));
        }

        @Test
        @DisplayName("should convert to long")
        void shouldConvertToLong() {
            Map<String, Object> config = new HashMap<>();
            config.put("extractRules", List.of(
                Map.of(
                    "target", "longValue",
                    "fixedValue", "9876543210",
                    "valueType", "long"
                )
            ));

            Map<String, Object> written = new HashMap<>();
            PluginContext context = PluginContext.builder()
                .pluginConfig(config)
                .contextReader(createEmptyReader())
                .factWriter(written::put)
                .build();

            PluginResult result = plugin.execute(context);

            assertTrue(result.isSuccess());
            assertEquals(9876543210L, written.get("longValue"));
            assertInstanceOf(Long.class, written.get("longValue"));
        }

        @Test
        @DisplayName("should convert to double")
        void shouldConvertToDouble() {
            Map<String, Object> config = new HashMap<>();
            config.put("extractRules", List.of(
                Map.of(
                    "target", "doubleValue",
                    "fixedValue", "3.14159",
                    "valueType", "double"
                )
            ));

            Map<String, Object> written = new HashMap<>();
            PluginContext context = PluginContext.builder()
                .pluginConfig(config)
                .contextReader(createEmptyReader())
                .factWriter(written::put)
                .build();

            PluginResult result = plugin.execute(context);

            assertTrue(result.isSuccess());
            assertEquals(3.14159, written.get("doubleValue"));
            assertInstanceOf(Double.class, written.get("doubleValue"));
        }

        @Test
        @DisplayName("should convert to boolean")
        void shouldConvertToBoolean() {
            Map<String, Object> config = new HashMap<>();
            config.put("extractRules", List.of(
                Map.of(
                    "target", "boolValue",
                    "fixedValue", "true",
                    "valueType", "boolean"
                )
            ));

            Map<String, Object> written = new HashMap<>();
            PluginContext context = PluginContext.builder()
                .pluginConfig(config)
                .contextReader(createEmptyReader())
                .factWriter(written::put)
                .build();

            PluginResult result = plugin.execute(context);

            assertTrue(result.isSuccess());
            assertEquals(true, written.get("boolValue"));
            assertInstanceOf(Boolean.class, written.get("boolValue"));
        }

        @Test
        @DisplayName("should fail gracefully on invalid integer conversion")
        void shouldFailOnInvalidIntegerConversion() {
            Map<String, Object> config = new HashMap<>();
            config.put("extractRules", List.of(
                Map.of(
                    "target", "intValue",
                    "fixedValue", "not-a-number",
                    "valueType", "int"
                )
            ));

            PluginContext context = PluginContext.builder()
                .pluginConfig(config)
                .contextReader(createEmptyReader())
                .factWriter((k, v) -> {})
                .build();

            PluginResult result = plugin.execute(context);

            assertFalse(result.isSuccess());
            assertTrue(result.getErrorMessage().contains("数据提取失败"));
        }
    }

    @Nested
    @DisplayName("position extraction")
    class PositionExtraction {

        @Test
        @DisplayName("should extract by start and end index")
        void shouldExtractByStartEndIndex() {
            Map<String, Object> config = new HashMap<>();
            config.put("extractRules", List.of(
                Map.of(
                    "target", "extracted",
                    "startIndex", 6,
                    "endIndex", 11,
                    "extractType", "position"
                )
            ));

            DocNodeContext nodeContext = DocNodeContext.create(1L, "node1", 1L);
            nodeContext.setUnstructuredContent(Map.of("text", "prefixVALUEsuffix"));
            NodeContextReader reader = new NodeContextReader(List.of(nodeContext));

            Map<String, Object> written = new HashMap<>();
            PluginContext context = PluginContext.builder()
                .pluginConfig(config)
                .contextReader(reader)
                .factWriter(written::put)
                .build();

            PluginResult result = plugin.execute(context);

            assertTrue(result.isSuccess());
            assertEquals("VALUE", written.get("extracted"));
        }

        @Test
        @DisplayName("should extract by markers")
        void shouldExtractByMarkers() {
            Map<String, Object> config = new HashMap<>();
            config.put("extractRules", List.of(
                Map.of(
                    "target", "extracted",
                    "startMarker", "[START]",
                    "endMarker", "[END]",
                    "extractType", "position"
                )
            ));

            DocNodeContext nodeContext = DocNodeContext.create(1L, "node1", 1L);
            nodeContext.setUnstructuredContent(Map.of("text", "prefix[START]TARGET[END]suffix"));
            NodeContextReader reader = new NodeContextReader(List.of(nodeContext));

            Map<String, Object> written = new HashMap<>();
            PluginContext context = PluginContext.builder()
                .pluginConfig(config)
                .contextReader(reader)
                .factWriter(written::put)
                .build();

            PluginResult result = plugin.execute(context);

            assertTrue(result.isSuccess());
            assertEquals("TARGET", written.get("extracted"));
        }

        @Test
        @DisplayName("should extract by length from start")
        void shouldExtractByLength() {
            Map<String, Object> config = new HashMap<>();
            config.put("extractRules", List.of(
                Map.of(
                    "target", "extracted",
                    "startIndex", 0,
                    "length", 5,
                    "extractType", "position"
                )
            ));

            DocNodeContext nodeContext = DocNodeContext.create(1L, "node1", 1L);
            nodeContext.setUnstructuredContent(Map.of("text", "HELLO world"));
            NodeContextReader reader = new NodeContextReader(List.of(nodeContext));

            Map<String, Object> written = new HashMap<>();
            PluginContext context = PluginContext.builder()
                .pluginConfig(config)
                .contextReader(reader)
                .factWriter(written::put)
                .build();

            PluginResult result = plugin.execute(context);

            assertTrue(result.isSuccess());
            assertEquals("HELLO", written.get("extracted"));
        }
    }

    @Nested
    @DisplayName("xpath extraction")
    class XpathExtraction {

        @Test
        @DisplayName("should extract using xpath")
        void shouldExtractUsingXpath() {
            Map<String, Object> config = new HashMap<>();
            config.put("extractRules", List.of(
                Map.of(
                    "target", "extracted",
                    "xpath", "//value/text()",
                    "extractType", "xpath"
                )
            ));

            DocNodeContext nodeContext = DocNodeContext.create(1L, "node1", 1L);
            nodeContext.setUnstructuredContent(Map.of("text", "<root><value>TEST</value></root>"));
            NodeContextReader reader = new NodeContextReader(List.of(nodeContext));

            Map<String, Object> written = new HashMap<>();
            PluginContext context = PluginContext.builder()
                .pluginConfig(config)
                .contextReader(reader)
                .factWriter(written::put)
                .build();

            PluginResult result = plugin.execute(context);

            assertTrue(result.isSuccess());
            assertEquals("TEST", written.get("extracted"));
        }

        @Test
        @DisplayName("should fail with malformed xml")
        void shouldFailWithMalformedXml() {
            Map<String, Object> config = new HashMap<>();
            config.put("extractRules", List.of(
                Map.of(
                    "target", "extracted",
                    "xpath", "//value",
                    "extractType", "xpath"
                )
            ));

            DocNodeContext nodeContext = DocNodeContext.create(1L, "node1", 1L);
            nodeContext.setUnstructuredContent(Map.of("text", "not valid <xml"));
            NodeContextReader reader = new NodeContextReader(List.of(nodeContext));

            PluginContext context = PluginContext.builder()
                .pluginConfig(config)
                .contextReader(reader)
                .factWriter((k, v) -> {})
                .build();

            PluginResult result = plugin.execute(context);

            assertFalse(result.isSuccess());
            assertTrue(result.getErrorMessage().contains("数据提取失败"));
        }

        @Test
        @DisplayName("should fail when xpath expression is missing")
        void shouldFailWhenXpathMissing() {
            Map<String, Object> config = new HashMap<>();
            config.put("extractRules", List.of(
                Map.of(
                    "target", "extracted",
                    "extractType", "xpath"
                )
            ));

            DocNodeContext nodeContext = DocNodeContext.create(1L, "node1", 1L);
            nodeContext.setUnstructuredContent(Map.of("text", "<root>content</root>"));
            NodeContextReader reader = new NodeContextReader(List.of(nodeContext));

            PluginContext context = PluginContext.builder()
                .pluginConfig(config)
                .contextReader(reader)
                .factWriter((k, v) -> {})
                .build();

            PluginResult result = plugin.execute(context);

            assertFalse(result.isSuccess());
            assertTrue(result.getErrorMessage().contains("xpath 规则缺少 expression"));
        }
    }

    @Nested
    @DisplayName("fixed value and target resolution")
    class FixedValueAndTarget {

        @Test
        @DisplayName("should use fixedValue directly")
        void shouldUseFixedValue() {
            Map<String, Object> config = new HashMap<>();
            config.put("extractRules", List.of(
                Map.of(
                    "target", "outputField",
                    "fixedValue", "fixed-data"
                )
            ));

            Map<String, Object> written = new HashMap<>();
            PluginContext context = PluginContext.builder()
                .pluginConfig(config)
                .contextReader(createEmptyReader())
                .factWriter(written::put)
                .build();

            PluginResult result = plugin.execute(context);

            assertTrue(result.isSuccess());
            assertEquals("fixed-data", written.get("outputField"));
        }

        @Test
        @DisplayName("should resolve target from field alias")
        void shouldResolveTargetFromFieldAlias() {
            Map<String, Object> config = new HashMap<>();
            config.put("extractRules", List.of(
                Map.of(
                    "field", "outputField",
                    "fixedValue", "data"
                )
            ));

            Map<String, Object> written = new HashMap<>();
            PluginContext context = PluginContext.builder()
                .pluginConfig(config)
                .contextReader(createEmptyReader())
                .factWriter(written::put)
                .build();

            PluginResult result = plugin.execute(context);

            assertTrue(result.isSuccess());
            assertEquals("data", written.get("outputField"));
        }

        @Test
        @DisplayName("should resolve target from fieldName alias")
        void shouldResolveTargetFromFieldNameAlias() {
            Map<String, Object> config = new HashMap<>();
            config.put("extractRules", List.of(
                Map.of(
                    "fieldName", "outputField",
                    "fixedValue", "data"
                )
            ));

            Map<String, Object> written = new HashMap<>();
            PluginContext context = PluginContext.builder()
                .pluginConfig(config)
                .contextReader(createEmptyReader())
                .factWriter(written::put)
                .build();

            PluginResult result = plugin.execute(context);

            assertTrue(result.isSuccess());
            assertEquals("data", written.get("outputField"));
        }

        @Test
        @DisplayName("should skip when target is blank")
        void shouldSkipWhenTargetIsBlank() {
            Map<String, Object> config = new HashMap<>();
            config.put("extractRules", List.of(
                Map.of(
                    "fixedValue", "data"
                )
            ));

            Map<String, Object> written = new HashMap<>();
            PluginContext context = PluginContext.builder()
                .pluginConfig(config)
                .contextReader(createEmptyReader())
                .factWriter(written::put)
                .build();

            PluginResult result = plugin.execute(context);

            assertTrue(result.isSuccess());
            assertTrue(written.isEmpty());
        }

        @Test
        @DisplayName("should skip null or empty rule")
        void shouldSkipNullOrEmptyRule() {
            Map<String, Object> config = new HashMap<>();
            List<Map<String, Object>> rules = new ArrayList<>();
            rules.add(null);
            rules.add(Collections.emptyMap());
            rules.add(Map.of("target", "validField", "fixedValue", "valid"));
            config.put("extractRules", rules);

            Map<String, Object> written = new HashMap<>();
            PluginContext context = PluginContext.builder()
                .pluginConfig(config)
                .contextReader(createEmptyReader())
                .factWriter(written::put)
                .build();

            PluginResult result = plugin.execute(context);

            assertTrue(result.isSuccess());
            assertEquals(1, written.size());
            assertEquals("valid", written.get("validField"));
        }
    }

    @Nested
    @DisplayName("plugin metadata")
    class PluginMetadata {

        @Test
        @DisplayName("should return correct plugin id")
        void shouldReturnCorrectPluginId() {
            assertEquals("data-extract", plugin.getPluginId());
        }

        @Test
        @DisplayName("should return correct plugin name")
        void shouldReturnCorrectPluginName() {
            assertEquals("数据提取插件", plugin.getPluginName());
        }

        @Test
        @DisplayName("should return correct plugin type")
        void shouldReturnCorrectPluginType() {
            assertEquals(PluginType.DATA_EXTRACT, plugin.getPluginType());
        }

        @Test
        @DisplayName("should expose wildcard input definition")
        void shouldExposeWildcardInputDefinition() {
            assertEquals(1, plugin.getInputFields().size());
            assertEquals("*", plugin.getInputFields().get(0).getName());
            assertEquals("dynamic", plugin.getInputFields().get(0).getType());
        }

        @Test
        @DisplayName("should expose wildcard output definition")
        void shouldExposeWildcardOutputDefinition() {
            assertEquals(1, plugin.getOutputFields().size());
            assertEquals("*", plugin.getOutputFields().get(0).getName());
            assertEquals("dynamic", plugin.getOutputFields().get(0).getType());
        }
    }

    // Helper methods

    private NodeContextReader createEmptyReader() {
        DocNodeContext nodeContext = DocNodeContext.create(1L, "node1", 1L);
        return new NodeContextReader(List.of(nodeContext));
    }
}
