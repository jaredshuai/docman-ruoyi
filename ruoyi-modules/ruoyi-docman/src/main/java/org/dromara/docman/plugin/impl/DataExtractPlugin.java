package org.dromara.docman.plugin.impl;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.dromara.docman.plugin.DocumentPlugin;
import org.dromara.docman.plugin.FieldDef;
import org.dromara.docman.plugin.PluginContext;
import org.dromara.docman.plugin.PluginResult;
import org.dromara.docman.plugin.PluginType;
import org.dromara.docman.plugin.annotation.DocPlugin;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.StringReader;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

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
            if (config == null || config.isEmpty()) {
                return PluginResult.fail("缺少插件配置");
            }

            List<Map<String, Object>> extractRules = (List<Map<String, Object>>) config.get("extractRules");
            if (extractRules == null || extractRules.isEmpty()) {
                return PluginResult.fail("缺少必要配置: extractRules");
            }

            Map<String, Object> allStructured = context.getContextReader().getAllReadableFields();
            Map<String, String> allUnstructured = context.getContextReader().getAllUnstructuredContent();
            String mergedUnstructured = allUnstructured.values().stream()
                .filter(StrUtil::isNotBlank)
                .collect(Collectors.joining("\n\n"));
            int extracted = 0;

            for (Map<String, Object> rule : extractRules) {
                if (rule == null || rule.isEmpty()) {
                    continue;
                }
                String target = resolveTarget(rule);
                if (StrUtil.isBlank(target)) {
                    continue;
                }

                Object value = resolveRuleValue(rule, allStructured, allUnstructured, mergedUnstructured);
                if (value == null || (value instanceof String str && StrUtil.isBlank(str))) {
                    continue;
                }

                String targetScope = StrUtil.blankToDefault(getString(rule, "targetScope"), "fact");
                if ("content".equalsIgnoreCase(targetScope)) {
                    context.getContentWriter().accept(target, String.valueOf(value));
                } else {
                    writeStructured(context, targetScope, target, value);
                }
                extracted++;
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

    private String resolveTarget(Map<String, Object> rule) {
        String target = getString(rule, "target");
        if (StrUtil.isBlank(target)) {
            target = getString(rule, "field");
        }
        if (StrUtil.isBlank(target)) {
            target = getString(rule, "fieldName");
        }
        return target;
    }

    private Object resolveRuleValue(Map<String, Object> rule, Map<String, Object> allStructured,
                                    Map<String, String> allUnstructured, String mergedUnstructured) throws Exception {
        String fixedValue = getString(rule, "fixedValue");
        if (StrUtil.isNotBlank(fixedValue)) {
            return convertValue(rule, fixedValue);
        }

        ExtractionMode extractionMode = resolveExtractionMode(rule);
        return switch (extractionMode) {
            case COPY -> resolveCopiedValue(rule, allStructured, allUnstructured);
            case REGEX -> convertValue(rule, extractByRegex(resolveSourceText(rule, allStructured, allUnstructured, mergedUnstructured), rule));
            case XPATH -> convertValue(rule, extractByXpath(resolveSourceText(rule, allStructured, allUnstructured, mergedUnstructured), rule));
            case POSITION -> convertValue(rule, extractByPosition(resolveSourceText(rule, allStructured, allUnstructured, mergedUnstructured), rule));
        };
    }

    private ExtractionMode resolveExtractionMode(Map<String, Object> rule) {
        String mode = StrUtil.blankToDefault(getString(rule, "extractType"), getString(rule, "ruleType"));
        if (StrUtil.isBlank(mode)) {
            if (StrUtil.isNotBlank(getString(rule, "pattern")) || StrUtil.isNotBlank(getString(rule, "regex"))) {
                return ExtractionMode.REGEX;
            }
            if (StrUtil.isNotBlank(getString(rule, "xpath")) || StrUtil.isNotBlank(getString(rule, "expression"))) {
                return ExtractionMode.XPATH;
            }
            if (rule.containsKey("startIndex") || rule.containsKey("endIndex")
                || rule.containsKey("length") || StrUtil.isNotBlank(getString(rule, "startMarker"))
                || StrUtil.isNotBlank(getString(rule, "endMarker"))) {
                return ExtractionMode.POSITION;
            }
            return ExtractionMode.COPY;
        }
        return switch (mode.toLowerCase()) {
            case "regex" -> ExtractionMode.REGEX;
            case "xpath" -> ExtractionMode.XPATH;
            case "position" -> ExtractionMode.POSITION;
            default -> ExtractionMode.COPY;
        };
    }

    private Object resolveCopiedValue(Map<String, Object> rule, Map<String, Object> allStructured, Map<String, String> allUnstructured) {
        String source = getString(rule, "source");
        String sourceType = StrUtil.blankToDefault(getString(rule, "sourceType"), "structured");
        if (StrUtil.isBlank(source)) {
            return null;
        }
        if ("unstructured".equalsIgnoreCase(sourceType) || allUnstructured.containsKey(source)) {
            return allUnstructured.get(source);
        }
        return allStructured.get(source);
    }

    private String resolveSourceText(Map<String, Object> rule, Map<String, Object> allStructured,
                                     Map<String, String> allUnstructured, String mergedUnstructured) {
        String source = getString(rule, "source");
        String sourceType = StrUtil.blankToDefault(getString(rule, "sourceType"), "unstructured");
        if (StrUtil.isBlank(source)) {
            return mergedUnstructured;
        }
        if ("structured".equalsIgnoreCase(sourceType)) {
            Object value = allStructured.get(source);
            return value == null ? null : String.valueOf(value);
        }
        String unstructured = allUnstructured.get(source);
        if (unstructured != null) {
            return unstructured;
        }
        Object structuredFallback = allStructured.get(source);
        return structuredFallback == null ? null : String.valueOf(structuredFallback);
    }

    private String extractByRegex(String sourceText, Map<String, Object> rule) {
        if (StrUtil.isBlank(sourceText)) {
            return null;
        }
        String patternText = StrUtil.blankToDefault(getString(rule, "pattern"), getString(rule, "regex"));
        if (StrUtil.isBlank(patternText)) {
            throw new IllegalArgumentException("regex 规则缺少 pattern");
        }

        Pattern pattern;
        try {
            pattern = Pattern.compile(patternText, resolveRegexFlags(getString(rule, "flags")));
        } catch (PatternSyntaxException ex) {
            throw new IllegalArgumentException("regex 规则不合法: " + patternText, ex);
        }

        Matcher matcher = pattern.matcher(sourceText);
        int matchIndex = getInt(rule, "matchIndex", 0);
        for (int index = 0; matcher.find(); index++) {
            if (index != matchIndex) {
                continue;
            }
            String groupName = getString(rule, "groupName");
            String extracted;
            if (StrUtil.isNotBlank(groupName)) {
                extracted = matcher.group(groupName);
            } else {
                int group = getInt(rule, "group", matcher.groupCount() > 0 ? 1 : 0);
                extracted = matcher.group(group);
            }
            return normalizeResult(extracted, getBoolean(rule, "trim", true));
        }
        return null;
    }

    private String extractByXpath(String sourceText, Map<String, Object> rule) throws Exception {
        if (StrUtil.isBlank(sourceText)) {
            return null;
        }
        String expression = StrUtil.blankToDefault(getString(rule, "xpath"), getString(rule, "expression"));
        if (StrUtil.isBlank(expression)) {
            throw new IllegalArgumentException("xpath 规则缺少 expression");
        }

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);

        Document document = factory.newDocumentBuilder().parse(new InputSource(new StringReader(sourceText)));
        String extracted = (String) XPathFactory.newInstance().newXPath()
            .evaluate(expression, document, XPathConstants.STRING);
        return normalizeResult(extracted, getBoolean(rule, "trim", true));
    }

    private String extractByPosition(String sourceText, Map<String, Object> rule) {
        if (StrUtil.isBlank(sourceText)) {
            return null;
        }

        int start = 0;
        String startMarker = getString(rule, "startMarker");
        if (StrUtil.isNotBlank(startMarker)) {
            int markerIndex = sourceText.indexOf(startMarker);
            if (markerIndex < 0) {
                return null;
            }
            start = markerIndex + (getBoolean(rule, "includeStart", false) ? 0 : startMarker.length());
        } else if (rule.containsKey("startIndex")) {
            start = Math.max(0, getInt(rule, "startIndex", 0));
        }

        int end;
        if (rule.containsKey("length")) {
            end = Math.min(sourceText.length(), start + Math.max(0, getInt(rule, "length", 0)));
        } else {
            String endMarker = getString(rule, "endMarker");
            if (StrUtil.isNotBlank(endMarker)) {
                int markerIndex = sourceText.indexOf(endMarker, Math.max(0, start));
                if (markerIndex < 0) {
                    return null;
                }
                end = markerIndex + (getBoolean(rule, "includeEnd", false) ? endMarker.length() : 0);
            } else if (rule.containsKey("endIndex")) {
                end = Math.min(sourceText.length(), Math.max(start, getInt(rule, "endIndex", sourceText.length())));
            } else {
                end = sourceText.length();
            }
        }

        if (start >= sourceText.length() || start >= end) {
            return null;
        }
        return normalizeResult(sourceText.substring(start, end), getBoolean(rule, "trim", true));
    }

    private Object convertValue(Map<String, Object> rule, Object value) {
        if (value == null) {
            return null;
        }
        String valueType = StrUtil.blankToDefault(getString(rule, "valueType"), "string");
        if (!(value instanceof String stringValue)) {
            return value;
        }
        return switch (valueType.toLowerCase()) {
            case "int", "integer" -> Integer.parseInt(stringValue);
            case "long" -> Long.parseLong(stringValue);
            case "double" -> Double.parseDouble(stringValue);
            case "boolean", "bool" -> Boolean.parseBoolean(stringValue);
            default -> stringValue;
        };
    }

    private int resolveRegexFlags(String flags) {
        if (StrUtil.isBlank(flags)) {
            return 0;
        }
        int resolved = 0;
        String normalized = flags.toLowerCase();
        if (normalized.contains("i")) {
            resolved |= Pattern.CASE_INSENSITIVE;
        }
        if (normalized.contains("m")) {
            resolved |= Pattern.MULTILINE;
        }
        if (normalized.contains("s")) {
            resolved |= Pattern.DOTALL;
        }
        return resolved;
    }

    private String normalizeResult(String value, boolean trim) {
        if (value == null) {
            return null;
        }
        return trim ? StrUtil.trim(value) : value;
    }

    private String getString(Map<String, Object> rule, String key) {
        Object value = rule.get(key);
        return value == null ? null : String.valueOf(value);
    }

    private int getInt(Map<String, Object> rule, String key, int defaultValue) {
        Object value = rule.get(key);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        String stringValue = String.valueOf(value);
        return StrUtil.isBlank(stringValue) ? defaultValue : Integer.parseInt(stringValue.trim());
    }

    private boolean getBoolean(Map<String, Object> rule, String key, boolean defaultValue) {
        Object value = rule.get(key);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Boolean bool) {
            return bool;
        }
        String stringValue = String.valueOf(value);
        return StrUtil.isBlank(stringValue) ? defaultValue : Boolean.parseBoolean(stringValue.trim());
    }

    private enum ExtractionMode {
        COPY,
        REGEX,
        XPATH,
        POSITION
    }
}
