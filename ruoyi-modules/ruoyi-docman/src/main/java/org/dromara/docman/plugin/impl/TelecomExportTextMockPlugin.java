package org.dromara.docman.plugin.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.docman.application.port.out.DocumentStoragePort;
import org.dromara.docman.plugin.DocumentPlugin;
import org.dromara.docman.plugin.FieldDef;
import org.dromara.docman.plugin.PluginContext;
import org.dromara.docman.plugin.PluginResult;
import org.dromara.docman.plugin.PluginType;
import org.dromara.docman.plugin.annotation.DocPlugin;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@DocPlugin("telecom-export-text-mock")
@RequiredArgsConstructor
public class TelecomExportTextMockPlugin implements DocumentPlugin {

    private final DocumentStoragePort documentStoragePort;

    @Override
    public String getPluginId() {
        return "telecom-export-text-mock";
    }

    @Override
    public String getPluginName() {
        return "电信文本导出 Mock 插件";
    }

    @Override
    public PluginType getPluginType() {
        return PluginType.TEXT_EXPORT;
    }

    @Override
    public List<FieldDef> getInputFields() {
        return List.of(
            FieldDef.builder().name("estimateAmount").type("number").required(false).description("估算金额").build()
        );
    }

    @Override
    public List<FieldDef> getOutputFields() {
        return List.of(
            FieldDef.builder().name("artifact").type("file").required(true).description("导出文本产物").build()
        );
    }

    @Override
    public PluginResult execute(PluginContext context) {
        try {
            Object estimateAmount = context.getContextReader().getProcessVariable("estimateAmount");
            String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            String fileName = dateStr + "_" + context.getProjectName() + "_mock_export.txt";
            String nasPath = context.getNasBasePath() + "/exports/" + fileName;
            String body = """
                电信文本导出 Mock 结果
                项目: %s
                节点: %s
                估算金额: %s
                """.formatted(context.getProjectName(), context.getNodeCode(), estimateAmount == null ? "-" : estimateAmount);
            Long ossId = documentStoragePort.store(nasPath, body.getBytes(StandardCharsets.UTF_8), fileName, "text/plain;charset=UTF-8").storageRecordId();
            return PluginResult.ok(List.of(
                PluginResult.GeneratedFile.builder()
                    .fileName(fileName)
                    .nasPath(nasPath)
                    .ossId(ossId)
                    .build()
            ));
        } catch (Exception e) {
            log.error("电信文本导出 mock 插件执行失败", e);
            return PluginResult.fail("文本导出 mock 执行失败: " + e.getMessage());
        }
    }
}
