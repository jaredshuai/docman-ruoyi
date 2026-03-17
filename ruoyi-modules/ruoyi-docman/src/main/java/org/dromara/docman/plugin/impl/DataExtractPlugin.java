package org.dromara.docman.plugin.impl;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.dromara.docman.plugin.*;
import org.dromara.docman.plugin.annotation.DocPlugin;

import java.util.List;
import java.util.Map;

@Slf4j
@DocPlugin("data-extract")
public class DataExtractPlugin implements DocumentPlugin {

    @Override
    public String getPluginId() { return "data-extract"; }

    @Override
    public String getPluginName() { return "数据提取插件"; }

    @Override
    public PluginType getPluginType() { return PluginType.DATA_EXTRACT; }

    @Override
    public List<FieldDef> getInputFields() {
        return List.of(FieldDef.builder().name("*").type("dynamic").description("根据 extractRules 动态读取").build());
    }

    @Override
    public List<FieldDef> getOutputFields() {
        return List.of(FieldDef.builder().name("*").type("dynamic").description("根据 extractRules 动态写入").build());
    }

    @Override
    @SuppressWarnings("unchecked")
    public PluginResult execute(PluginContext context) {
        try {
            Map<String, Object> config = context.getPluginConfig();
            List<Map<String, String>> extractRules = (List<Map<String, String>>) config.get("extractRules");
            if (extractRules == null || extractRules.isEmpty()) {
                return PluginResult.fail("缺少必要配置: extractRules");
            }

            Map<String, Object> allStructured = context.getContextReader().getAllReadableFields();
            Map<String, String> allUnstructured = context.getContextReader().getAllUnstructuredContent();
            int extracted = 0;

            for (Map<String, String> rule : extractRules) {
                String source = rule.get("source");
                String target = rule.get("target");
                String type = rule.getOrDefault("type", "structured");
                String targetScope = rule.getOrDefault("targetScope", "fact");
                String fixedValue = rule.get("fixedValue");

                if (StrUtil.isBlank(target)) continue;

                if ("structured".equals(type)) {
                    Object value = StrUtil.isNotBlank(fixedValue) ? fixedValue
                        : (StrUtil.isNotBlank(source) ? allStructured.get(source) : null);
                    if (value != null) {
                        writeStructured(context, targetScope, target, value);
                        extracted++;
                    }
                } else if ("unstructured".equals(type)) {
                    String value = StrUtil.isNotBlank(fixedValue) ? fixedValue
                        : (StrUtil.isNotBlank(source) ? allUnstructured.get(source) : null);
                    if (value != null) {
                        context.getContentWriter().accept(target, value);
                        extracted++;
                    }
                }
            }

            log.info("数据提取完成，共提取 {} 个字段", extracted);
            return PluginResult.ok();
        } catch (Exception e) {
            log.error("数据提取插件执行失败", e);
            return PluginResult.fail("数据提取失败: " + e.getMessage());
        }
    }

    private void writeStructured(PluginContext context, String targetScope, String target, Object value) {
        switch (targetScope) {
            case "process" -> context.getProcessWriter().accept(target, value);
            case "node" -> context.getNodeWriter().accept(target, value);
            default -> context.getFactWriter().accept(target, value);
        }
    }
}
