package org.dromara.docman.plugin.impl;

import org.dromara.docman.application.port.out.DocumentStoragePort;
import org.dromara.docman.application.port.out.KnowledgeSearchPort;
import org.dromara.docman.application.port.out.LlmGeneratePort;
import org.dromara.docman.config.DocmanAiConfig;
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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Direct unit tests for AiGeneratePlugin without Spring context.
 */
@Tag("local")
class AiGeneratePluginTest {

    private AiGeneratePlugin plugin;
    private KnowledgeSearchPort mockKnowledgeSearchPort;
    private DocmanAiConfig mockAiConfig;
    private LlmGeneratePort mockLlmGeneratePort;
    private DocumentStoragePort mockDocumentStoragePort;

    @BeforeEach
    void setUp() {
        mockKnowledgeSearchPort = new MockKnowledgeSearchPort();
        mockAiConfig = new MockAiConfig();
        mockLlmGeneratePort = new MockLlmGeneratePort();
        mockDocumentStoragePort = new MockDocumentStoragePort();
        plugin = new AiGeneratePlugin(mockKnowledgeSearchPort, mockAiConfig, mockLlmGeneratePort, mockDocumentStoragePort);
    }

    @Nested
    @DisplayName("missing promptTemplate branch")
    class MissingPromptTemplate {

        @Test
        @DisplayName("should fail when promptTemplate is null")
        void shouldFailWhenPromptTemplateIsNull() {
            Map<String, Object> config = new HashMap<>();
            config.put("promptTemplate", null);

            PluginContext context = PluginContext.builder()
                .pluginConfig(config)
                .build();

            PluginResult result = plugin.execute(context);

            assertFalse(result.isSuccess());
            assertEquals("缺少必要配置: promptTemplate", result.getErrorMessage());
        }

        @Test
        @DisplayName("should fail when promptTemplate is empty string")
        void shouldFailWhenPromptTemplateIsEmpty() {
            Map<String, Object> config = new HashMap<>();
            config.put("promptTemplate", "");

            PluginContext context = PluginContext.builder()
                .pluginConfig(config)
                .build();

            PluginResult result = plugin.execute(context);

            assertFalse(result.isSuccess());
            assertEquals("缺少必要配置: promptTemplate", result.getErrorMessage());
        }

        @Test
        @DisplayName("should fail when promptTemplate is blank string")
        void shouldFailWhenPromptTemplateIsBlank() {
            Map<String, Object> config = new HashMap<>();
            config.put("promptTemplate", "   ");

            PluginContext context = PluginContext.builder()
                .pluginConfig(config)
                .build();

            PluginResult result = plugin.execute(context);

            assertFalse(result.isSuccess());
            assertEquals("缺少必要配置: promptTemplate", result.getErrorMessage());
        }

        @Test
        @DisplayName("should fail when promptTemplate key is missing from config")
        void shouldFailWhenPromptTemplateKeyMissing() {
            Map<String, Object> config = new HashMap<>();
            config.put("outputFileName", "test");

            PluginContext context = PluginContext.builder()
                .pluginConfig(config)
                .build();

            PluginResult result = plugin.execute(context);

            assertFalse(result.isSuccess());
            assertEquals("缺少必要配置: promptTemplate", result.getErrorMessage());
        }
    }

    @Nested
    @DisplayName("empty AI result branch")
    class EmptyAiResult {

        @Test
        @DisplayName("should fail when LLM returns null")
        void shouldFailWhenLlmReturnsNull() {
            ((MockLlmGeneratePort) mockLlmGeneratePort).setReturnValue(null);

            Map<String, Object> config = createValidConfig();

            PluginContext context = createValidContext(config);

            PluginResult result = plugin.execute(context);

            assertFalse(result.isSuccess());
            assertEquals("AI生成内容为空", result.getErrorMessage());
        }

        @Test
        @DisplayName("should fail when LLM returns empty string")
        void shouldFailWhenLlmReturnsEmpty() {
            ((MockLlmGeneratePort) mockLlmGeneratePort).setReturnValue("");

            Map<String, Object> config = createValidConfig();

            PluginContext context = createValidContext(config);

            PluginResult result = plugin.execute(context);

            assertFalse(result.isSuccess());
            assertEquals("AI生成内容为空", result.getErrorMessage());
        }

        @Test
        @DisplayName("should fail when LLM returns blank string")
        void shouldFailWhenLlmReturnsBlank() {
            ((MockLlmGeneratePort) mockLlmGeneratePort).setReturnValue("   ");

            Map<String, Object> config = createValidConfig();

            PluginContext context = createValidContext(config);

            PluginResult result = plugin.execute(context);

            assertFalse(result.isSuccess());
            assertEquals("AI生成内容为空", result.getErrorMessage());
        }
    }

    @Nested
    @DisplayName("successful generation path")
    class SuccessfulGeneration {

        @Test
        @DisplayName("should succeed with valid config and return generated content")
        void shouldSucceedWithValidConfig() {
            Map<String, Object> config = createValidConfig();

            PluginContext context = createValidContext(config);

            PluginResult result = plugin.execute(context);

            assertTrue(result.isSuccess());
            assertNull(result.getErrorMessage());
        }

        @Test
        @DisplayName("should write to context when writeToContext is true")
        void shouldWriteToContextWhenEnabled() {
            Map<String, Object> config = createValidConfig();
            config.put("writeToContext", true);

            Map<String, Object> writtenContent = new HashMap<>();
            PluginContext context = PluginContext.builder()
                .pluginConfig(config)
                .contextReader(createEmptyContextReader())
                .contentWriter((key, value) -> writtenContent.put(key, value))
                .build();

            PluginResult result = plugin.execute(context);

            assertTrue(result.isSuccess());
            assertEquals("AI generated test content", writtenContent.get("ai_generated_content"));
        }

        @Test
        @DisplayName("should not write to context when writeToContext is false")
        void shouldNotWriteToContextWhenDisabled() {
            Map<String, Object> config = createValidConfig();
            config.put("writeToContext", false);

            Map<String, Object> writtenContent = new HashMap<>();
            PluginContext context = PluginContext.builder()
                .pluginConfig(config)
                .contextReader(createEmptyContextReader())
                .contentWriter((key, value) -> writtenContent.put(key, value))
                .build();

            PluginResult result = plugin.execute(context);

            assertTrue(result.isSuccess());
            assertFalse(writtenContent.containsKey("ai_generated_content"));
        }

        @Test
        @DisplayName("should store file when outputFileName is provided")
        void shouldStoreFileWhenOutputFileNameProvided() {
            Map<String, Object> config = createValidConfig();
            config.put("outputFileName", "test-output");

            PluginContext context = PluginContext.builder()
                .pluginConfig(config)
                .contextReader(createEmptyContextReader())
                .nasBasePath("/projects/1")
                .archiveFolderName("archive")
                .build();

            PluginResult result = plugin.execute(context);

            assertTrue(result.isSuccess());
            assertNotNull(result.getGeneratedFiles());
            assertEquals(1, result.getGeneratedFiles().size());

            PluginResult.GeneratedFile file = result.getGeneratedFiles().get(0);
            assertNotNull(file.getFileName());
            assertTrue(file.getFileName().endsWith(".txt"));
            assertTrue(file.getFileName().contains("test-output"));
            assertNotNull(file.getNasPath());
            assertEquals(1L, file.getOssId());
        }

        @Test
        @DisplayName("should replace placeholders in prompt with context fields")
        void shouldReplacePlaceholdersInPrompt() {
            Map<String, Object> config = createValidConfig();
            config.put("promptTemplate", "Hello {name}, your task is {task}");

            DocNodeContext nodeContext = DocNodeContext.create(1L, "node1", 1L);
            nodeContext.setNodeVariables(Map.of("name", "Alice", "task", "testing"));
            NodeContextReader reader = new NodeContextReader(List.of(nodeContext));

            PluginContext context = PluginContext.builder()
                .pluginConfig(config)
                .contextReader(reader)
                .build();

            PluginResult result = plugin.execute(context);

            assertTrue(result.isSuccess());
            // Verify the prompt was processed (the mock captures the generated prompt)
            assertEquals("Hello Alice, your task is testing", ((MockLlmGeneratePort) mockLlmGeneratePort).getLastPrompt());
        }

        @Test
        @DisplayName("should use custom outputFormat from config")
        void shouldUseCustomOutputFormat() {
            Map<String, Object> config = createValidConfig();
            config.put("outputFileName", "test-output");
            config.put("outputFormat", "md");

            PluginContext context = PluginContext.builder()
                .pluginConfig(config)
                .contextReader(createEmptyContextReader())
                .nasBasePath("/projects/1")
                .archiveFolderName("archive")
                .build();

            PluginResult result = plugin.execute(context);

            assertTrue(result.isSuccess());
            PluginResult.GeneratedFile file = result.getGeneratedFiles().get(0);
            assertTrue(file.getFileName().endsWith(".md"));
        }
    }

    @Nested
    @DisplayName("knowledge search integration")
    class KnowledgeSearch {

        @Test
        @DisplayName("should search knowledge when knowledgeQuery is provided")
        void shouldSearchKnowledgeWhenQueryProvided() {
            Map<String, Object> config = createValidConfig();
            config.put("knowledgeQuery", "test query");
            config.put("knowledgeTopK", 3);

            PluginContext context = createValidContext(config);

            PluginResult result = plugin.execute(context);

            assertTrue(result.isSuccess());
            assertTrue(((MockKnowledgeSearchPort) mockKnowledgeSearchPort).wasSearchCalled());
        }

        @Test
        @DisplayName("should include reference text in prompt when knowledge found")
        void shouldIncludeReferenceTextInPrompt() {
            Map<String, Object> config = createValidConfig();
            config.put("knowledgeQuery", "test query");

            PluginContext context = createValidContext(config);

            PluginResult result = plugin.execute(context);

            assertTrue(result.isSuccess());
            String lastPrompt = ((MockLlmGeneratePort) mockLlmGeneratePort).getLastPrompt();
            assertTrue(lastPrompt.contains("参考资料"));
            assertTrue(lastPrompt.contains("Mock knowledge content"));
        }

        @Test
        @DisplayName("should skip knowledge search when knowledgeQuery is blank")
        void shouldSkipKnowledgeSearchWhenQueryBlank() {
            Map<String, Object> config = createValidConfig();
            config.put("knowledgeQuery", "   ");

            PluginContext context = createValidContext(config);

            PluginResult result = plugin.execute(context);

            assertTrue(result.isSuccess());
            assertFalse(((MockKnowledgeSearchPort) mockKnowledgeSearchPort).wasSearchCalled());
        }
    }

    @Nested
    @DisplayName("plugin metadata")
    class PluginMetadata {

        @Test
        @DisplayName("should return correct plugin id")
        void shouldReturnCorrectPluginId() {
            assertEquals("ai-generate", plugin.getPluginId());
        }

        @Test
        @DisplayName("should return correct plugin name")
        void shouldReturnCorrectPluginName() {
            assertEquals("AI生成插件", plugin.getPluginName());
        }

        @Test
        @DisplayName("should return correct plugin type")
        void shouldReturnCorrectPluginType() {
            assertEquals(PluginType.AI_GENERATE, plugin.getPluginType());
        }

        @Test
        @DisplayName("should return input fields definition")
        void shouldReturnInputFields() {
            assertNotNull(plugin.getInputFields());
            assertEquals(1, plugin.getInputFields().size());
            assertEquals("*", plugin.getInputFields().get(0).getName());
        }

        @Test
        @DisplayName("should return output fields definition")
        void shouldReturnOutputFields() {
            assertNotNull(plugin.getOutputFields());
            assertEquals(1, plugin.getOutputFields().size());
            assertEquals("ai_generated_content", plugin.getOutputFields().get(0).getName());
        }
    }

    // Helper methods

    private Map<String, Object> createValidConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("promptTemplate", "Test prompt");
        return config;
    }

    private PluginContext createValidContext(Map<String, Object> config) {
        return PluginContext.builder()
            .pluginConfig(config)
            .contextReader(createEmptyContextReader())
            .build();
    }

    private NodeContextReader createEmptyContextReader() {
        DocNodeContext nodeContext = DocNodeContext.create(1L, "node1", 1L);
        nodeContext.setNodeVariables(Collections.emptyMap());
        return new NodeContextReader(List.of(nodeContext));
    }

    // Mock implementations

    private static class MockKnowledgeSearchPort implements KnowledgeSearchPort {
        private boolean searchCalled = false;

        @Override
        public List<KnowledgeResult> search(String query, int topK) {
            searchCalled = true;
            return List.of(new KnowledgeResult("Mock knowledge content", "source1"));
        }

        public boolean wasSearchCalled() {
            return searchCalled;
        }
    }

    private static class MockAiConfig extends DocmanAiConfig {
        public MockAiConfig() {
            setMaxTokens(4096);
        }
    }

    private static class MockLlmGeneratePort implements LlmGeneratePort {
        private String returnValue = "AI generated test content";
        private String lastPrompt;

        @Override
        public String generate(String prompt, int maxTokens) {
            this.lastPrompt = prompt;
            return returnValue;
        }

        public void setReturnValue(String value) {
            this.returnValue = value;
        }

        public String getLastPrompt() {
            return lastPrompt;
        }
    }

    private static class MockDocumentStoragePort implements DocumentStoragePort {
        @Override
        public boolean ensureDirectory(String path) {
            return true;
        }

        @Override
        public byte[] load(String path) {
            return new byte[0];
        }

        @Override
        public StoredDocument store(String path, byte[] content, String fileName, String contentType) {
            return new StoredDocument(path, fileName, 1L);
        }
    }
}