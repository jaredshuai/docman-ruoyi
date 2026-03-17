package org.dromara.docman.plugin.impl;

import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.ss.util.CellReference;
import org.dromara.docman.application.port.out.DocumentStoragePort;
import org.dromara.docman.plugin.DocumentPlugin;
import org.dromara.docman.plugin.FieldDef;
import org.dromara.docman.plugin.PluginContext;
import org.dromara.docman.plugin.PluginResult;
import org.dromara.docman.plugin.PluginType;
import org.dromara.docman.plugin.annotation.DocPlugin;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@DocPlugin("excel-fill")
@RequiredArgsConstructor
public class ExcelFillPlugin implements DocumentPlugin {

    private final DocumentStoragePort documentStoragePort;

    @Override
    public String getPluginId() { return "excel-fill"; }

    @Override
    public String getPluginName() { return "Excel填充插件"; }

    @Override
    public PluginType getPluginType() { return PluginType.EXCEL_FILL; }

    @Override
    public List<FieldDef> getInputFields() {
        return List.of(FieldDef.builder().name("*").type("dynamic").description("根据 fieldMapping 动态读取").build());
    }

    @Override
    public List<FieldDef> getOutputFields() { return List.of(); }

    @Override
    @SuppressWarnings("unchecked")
    public PluginResult execute(PluginContext context) {
        try {
            Map<String, Object> config = context.getPluginConfig();
            if (config == null || config.isEmpty()) {
                return PluginResult.fail("缺少插件配置");
            }
            String templatePath = getConfigString(config, "templatePath");
            String outputFileName = getConfigString(config, "outputFileName");
            String sheetName = getConfigString(config, "sheetName");
            int sheetIndex = config.containsKey("sheetIndex") ? ((Number) config.get("sheetIndex")).intValue() : 0;
            Map<String, String> fieldMapping = normalizeFieldMapping((Map<String, Object>) config.get("fieldMapping"));

            if (StrUtil.isBlank(templatePath) || fieldMapping == null || fieldMapping.isEmpty()) {
                return PluginResult.fail("缺少必要配置: templatePath 或 fieldMapping");
            }

            Map<String, Object> allFields = context.getContextReader().getAllReadableFields();
            byte[] templateBytes = documentStoragePort.load(templatePath);

            byte[] fileBytes;
            try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(templateBytes));
                 ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
                Sheet sheet = resolveSheet(workbook, sheetName, sheetIndex);
                if (sheet == null) {
                    return PluginResult.fail("模板工作表不存在: " + (StrUtil.isNotBlank(sheetName) ? sheetName : sheetIndex));
                }

                for (Map.Entry<String, String> entry : fieldMapping.entrySet()) {
                    Object value = allFields.get(entry.getKey());
                    if (value == null) {
                        continue;
                    }
                    CellReference ref = new CellReference(entry.getValue());
                    Row row = sheet.getRow(ref.getRow());
                    if (row == null) {
                        row = sheet.createRow(ref.getRow());
                    }
                    Cell cell = row.getCell(ref.getCol());
                    if (cell == null) {
                        cell = row.createCell(ref.getCol());
                    }
                    setCellValue(cell, value);
                }

                workbook.getCreationHelper().createFormulaEvaluator().evaluateAll();
                workbook.write(bos);
                fileBytes = bos.toByteArray();
            }

            String extension = resolveExtension(templatePath, outputFileName);
            String resolvedBaseName = resolveOutputBaseName(templatePath, outputFileName);
            String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            String fullFileName = String.format("%s_%s.%s", dateStr, resolvedBaseName, extension);
            String nasPath = buildOutputPath(context, fullFileName);

            Long ossId = documentStoragePort.store(nasPath, fileBytes, fullFileName, resolveContentType(extension))
                .storageRecordId();

            return PluginResult.ok(List.of(
                PluginResult.GeneratedFile.builder().fileName(fullFileName).nasPath(nasPath).ossId(ossId).build()
            ));
        } catch (Exception e) {
            log.error("Excel填充插件执行失败", e);
            return PluginResult.fail("Excel填充失败: " + e.getMessage());
        }
    }

    private String getConfigString(Map<String, Object> config, String key) {
        Object value = config.get(key);
        return value == null ? null : StrUtil.trim(String.valueOf(value));
    }

    private void setCellValue(Cell cell, Object value) {
        if (value instanceof Number num) {
            cell.setCellValue(num.doubleValue());
        } else if (value instanceof LocalDate localDate) {
            cell.setCellValue(localDate);
        } else if (value instanceof LocalDateTime localDateTime) {
            cell.setCellValue(Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant()));
        } else if (value instanceof Date date) {
            cell.setCellValue(date);
        } else if (value instanceof Boolean bool) {
            cell.setCellValue(bool);
        } else {
            cell.setCellValue(String.valueOf(value));
        }
    }

    private Map<String, String> normalizeFieldMapping(Map<String, Object> rawFieldMapping) {
        if (rawFieldMapping == null || rawFieldMapping.isEmpty()) {
            return Map.of();
        }
        Map<String, String> fieldMapping = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : rawFieldMapping.entrySet()) {
            String fieldName = StrUtil.trim(entry.getKey());
            String cellRef = entry.getValue() == null ? null : StrUtil.trim(String.valueOf(entry.getValue()));
            if (StrUtil.isNotBlank(fieldName) && StrUtil.isNotBlank(cellRef)) {
                fieldMapping.put(fieldName, cellRef);
            }
        }
        return fieldMapping;
    }

    private Sheet resolveSheet(Workbook workbook, String sheetName, int sheetIndex) {
        if (StrUtil.isNotBlank(sheetName)) {
            return workbook.getSheet(sheetName);
        }
        if (sheetIndex < 0 || sheetIndex >= workbook.getNumberOfSheets()) {
            return null;
        }
        return workbook.getSheetAt(sheetIndex);
    }

    private String resolveOutputBaseName(String templatePath, String outputFileName) {
        String candidate = StrUtil.isNotBlank(outputFileName) ? outputFileName : extractFileName(templatePath);
        String baseName = stripExtension(candidate);
        if (StrUtil.isBlank(baseName)) {
            throw new IllegalArgumentException("缺少可用的输出文件名");
        }
        return baseName;
    }

    private String resolveExtension(String templatePath, String outputFileName) {
        String extension = extractExtension(outputFileName);
        if (StrUtil.isBlank(extension)) {
            extension = extractExtension(templatePath);
        }
        return StrUtil.blankToDefault(extension, "xlsx");
    }

    private String buildOutputPath(PluginContext context, String fileName) {
        StringBuilder pathBuilder = new StringBuilder(StrUtil.removeSuffix(context.getNasBasePath(), "/"));
        if (StrUtil.isNotBlank(context.getArchiveFolderName())) {
            pathBuilder.append("/").append(StrUtil.removePrefix(context.getArchiveFolderName(), "/"));
        }
        pathBuilder.append("/").append(fileName);
        return pathBuilder.toString();
    }

    private String extractFileName(String path) {
        String normalized = StrUtil.removeSuffix(path, "/");
        int lastSlashIndex = normalized.lastIndexOf('/');
        return lastSlashIndex >= 0 ? normalized.substring(lastSlashIndex + 1) : normalized;
    }

    private String stripExtension(String fileName) {
        if (StrUtil.isBlank(fileName)) {
            return fileName;
        }
        int lastDotIndex = fileName.lastIndexOf('.');
        return lastDotIndex > 0 ? fileName.substring(0, lastDotIndex) : fileName;
    }

    private String extractExtension(String fileName) {
        if (StrUtil.isBlank(fileName)) {
            return null;
        }
        int lastDotIndex = fileName.lastIndexOf('.');
        return lastDotIndex > -1 && lastDotIndex < fileName.length() - 1 ? fileName.substring(lastDotIndex + 1) : null;
    }

    private String resolveContentType(String extension) {
        return "xls".equalsIgnoreCase(extension)
            ? "application/vnd.ms-excel"
            : "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    }
}
