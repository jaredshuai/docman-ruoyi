package org.dromara.docman.plugin.impl;

import lombok.extern.slf4j.Slf4j;
import org.dromara.docman.plugin.DocumentPlugin;
import org.dromara.docman.plugin.FieldDef;
import org.dromara.docman.plugin.PluginContext;
import org.dromara.docman.plugin.PluginResult;
import org.dromara.docman.plugin.PluginType;
import org.dromara.docman.plugin.annotation.DocPlugin;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@DocPlugin("telecom-estimate-mock")
public class TelecomEstimateMockPlugin implements DocumentPlugin {

    @Override
    public String getPluginId() {
        return "telecom-estimate-mock";
    }

    @Override
    public String getPluginName() {
        return "电信初步估算 Mock 插件";
    }

    @Override
    public PluginType getPluginType() {
        return PluginType.MOCK_ESTIMATE;
    }

    @Override
    public List<FieldDef> getInputFields() {
        return List.of(
            FieldDef.builder().name("drawingCount").type("number").required(false).description("图纸数量").build(),
            FieldDef.builder().name("visaCount").type("number").required(false).description("签证数量").build()
        );
    }

    @Override
    public List<FieldDef> getOutputFields() {
        return List.of(
            FieldDef.builder().name("estimateStatus").type("string").required(true).description("估算状态").build(),
            FieldDef.builder().name("estimateAmount").type("number").required(true).description("估算金额").build()
        );
    }

    @Override
    public PluginResult execute(PluginContext context) {
        try {
            Object drawingCount = context.getContextReader().getProcessVariable("drawingCount");
            Object visaCount = context.getContextReader().getProcessVariable("visaCount");
            int drawing = drawingCount instanceof Number num ? num.intValue() : 0;
            int visa = visaCount instanceof Number num ? num.intValue() : 0;
            BigDecimal amount = BigDecimal.valueOf(drawing * 1000L + visa * 500L);

            context.getProcessWriter().accept("estimateStatus", "mocked");
            context.getProcessWriter().accept("estimateAmount", amount);
            context.getContentWriter().accept("estimateSummary", "Mock estimate for project " + context.getProjectName() + ": " + amount);
            return PluginResult.ok();
        } catch (Exception e) {
            log.error("电信初步估算 mock 插件执行失败", e);
            return PluginResult.fail("估算 mock 执行失败: " + e.getMessage());
        }
    }
}
