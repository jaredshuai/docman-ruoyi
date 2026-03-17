package org.dromara.docman.plugin.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.docman.application.port.out.DocumentStoragePort;
import org.dromara.docman.application.port.out.KnowledgeSearchPort;
import org.dromara.docman.application.port.out.LlmGeneratePort;
import org.dromara.docman.config.DocmanAiConfig;
import org.dromara.docman.plugin.*;
import org.dromara.docman.plugin.annotation.DocPlugin;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@DocPlugin("ai-generate")
@RequiredArgsConstructor
public class AiGeneratePlugin implements DocumentPlugin {

    private final KnowledgeSearchPort knowledgeSearchPort;
    private final DocmanAiConfig aiConfig;
    private final LlmGeneratePort llmGeneratePort;
    private final DocumentStoragePort documentStoragePort;

    @Override
    public String getPluginId() { return "ai-generate"; }

    @Override
    public String getPluginName() { return "AI生成插件"; }

    @Override
    public PluginType getPluginType() { return PluginType.AI_GENERATE; }

    @Override
    public List<FieldDef> getInputFields() {
        return List.of(FieldDef.builder().name("*").type("dynamic").description("根据 promptTemplate 变量动态读取").build());
    }

    @Override
    public List<FieldDef> getOutputFields() {
        return List.of(FieldDef.builder().name("ai_generated_content").type("text").description("AI生成内容").build());
    }

    @Override
    public PluginResult execute(PluginContext context) {
        try {
            Map<String, Object> config = context.getPluginConfig();
            String promptTemplate = (String) config.get("promptTemplate");
            String knowledgeQuery = (String) config.get("knowledgeQuery");
            int knowledgeTopK = config.containsKey("knowledgeTopK") ? ((Number) config.get("knowledgeTopK")).intValue() : 5;
            String outputFileName = (String) config.get("outputFileName");
            boolean writeToContext = Boolean.TRUE.equals(config.get("writeToContext"));

            if (promptTemplate == null || promptTemplate.isBlank()) {
                return PluginResult.fail("缺少必要配置: promptTemplate");
            }

            Map<String, Object> allFields = context.getContextReader().getAllReadableFields();
            String prompt = promptTemplate;
            for (Map.Entry<String, Object> entry : allFields.entrySet()) {
                prompt = prompt.replace("{" + entry.getKey() + "}", String.valueOf(entry.getValue()));
            }

            String referenceText = "";
            if (knowledgeQuery != null && !knowledgeQuery.isBlank()) {
                String resolvedQuery = knowledgeQuery;
                for (Map.Entry<String, Object> entry : allFields.entrySet()) {
                    resolvedQuery = resolvedQuery.replace("{" + entry.getKey() + "}", String.valueOf(entry.getValue()));
                }
                List<KnowledgeSearchPort.KnowledgeResult> results = knowledgeSearchPort.search(resolvedQuery, knowledgeTopK);
                if (!results.isEmpty()) {
                    referenceText = results.stream().map(KnowledgeSearchPort.KnowledgeResult::content).collect(Collectors.joining("\n\n---\n\n"));
                }
            }

            String finalPrompt = referenceText.isEmpty() ? prompt
                : "以下是相关参考资料：\n\n" + referenceText + "\n\n---\n\n请基于以上参考资料，" + prompt;

            String generatedContent = llmGeneratePort.generate(finalPrompt, aiConfig.getMaxTokens());
            if (generatedContent == null || generatedContent.isBlank()) {
                return PluginResult.fail("AI生成内容为空");
            }

            if (writeToContext) {
                context.getContentWriter().accept("ai_generated_content", generatedContent);
            }

            if (outputFileName != null && !outputFileName.isBlank()) {
                String outputFormat = (String) config.getOrDefault("outputFormat", "txt");
                String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
                String fullFileName = String.format("%s_%s.%s", dateStr, outputFileName, outputFormat);
                String nasPath = context.getNasBasePath() + "/" + context.getArchiveFolderName() + "/" + fullFileName;
                byte[] fileBytes = generatedContent.getBytes(StandardCharsets.UTF_8);
                Long ossId = documentStoragePort.store(nasPath, fileBytes, fullFileName, "text/plain;charset=UTF-8").storageRecordId();

                return PluginResult.ok(List.of(
                    PluginResult.GeneratedFile.builder().fileName(fullFileName).nasPath(nasPath).ossId(ossId).build()
                ));
            }
            return PluginResult.ok();
        } catch (Exception e) {
            log.error("AI生成插件执行失败", e);
            return PluginResult.fail("AI生成失败: " + e.getMessage());
        }
    }
}
