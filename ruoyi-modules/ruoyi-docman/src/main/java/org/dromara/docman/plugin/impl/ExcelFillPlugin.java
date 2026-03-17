package org.dromara.docman.plugin.impl;

import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.dromara.docman.application.port.out.DocumentStoragePort;
import org.dromara.docman.plugin.*;
import org.dromara.docman.plugin.annotation.DocPlugin;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Date;
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
            String templatePath = (String) config.get("templatePath");
            String outputFileName = (String) config.get("outputFileName");
            int sheetIndex = config.containsKey("sheetIndex") ? ((Number) config.get("sheetIndex")).intValue() : 0;
            Map<String, String> fieldMapping = (Map<String, String>) config.get("fieldMapping");

            if (StrUtil.isBlank(templatePath) || fieldMapping == null || fieldMapping.isEmpty()) {
                return PluginResult.fail("缺少必要配置: templatePath 或 fieldMapping");
            }

            InputStream templateStream = getClass().getClassLoader().getResourceAsStream(templatePath);
            if (templateStream == null) {
                return PluginResult.fail("模板文件不存在: " + templatePath);
            }

            Workbook workbook = new XSSFWorkbook(templateStream);
            Sheet sheet = workbook.getSheetAt(sheetIndex);
            Map<String, Object> allFields = context.getContextReader().getAllReadableFields();

            for (Map.Entry<String, String> entry : fieldMapping.entrySet()) {
                Object value = allFields.get(entry.getKey());
                if (value != null) {
                    CellReference ref = new CellReference(entry.getValue());
                    Row row = sheet.getRow(ref.getRow());
                    if (row == null) row = sheet.createRow(ref.getRow());
                    Cell cell = row.getCell(ref.getCol());
                    if (cell == null) cell = row.createCell(ref.getCol());
                    setCellValue(cell, value);
                }
            }

            workbook.getCreationHelper().createFormulaEvaluator().evaluateAll();

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            workbook.write(bos);
            workbook.close();
            templateStream.close();

            byte[] fileBytes = bos.toByteArray();
            String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            String fullFileName = String.format("%s_%s.xlsx", dateStr, outputFileName);
            String nasPath = context.getNasBasePath() + "/" + context.getArchiveFolderName() + "/" + fullFileName;

            Long ossId = documentStoragePort.store(nasPath, fileBytes, fullFileName,
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet").storageRecordId();

            return PluginResult.ok(List.of(
                PluginResult.GeneratedFile.builder().fileName(fullFileName).nasPath(nasPath).ossId(ossId).build()
            ));
        } catch (Exception e) {
            log.error("Excel填充插件执行失败", e);
            return PluginResult.fail("Excel填充失败: " + e.getMessage());
        }
    }

    private void setCellValue(Cell cell, Object value) {
        if (value instanceof Number num) {
            cell.setCellValue(num.doubleValue());
        } else if (value instanceof Date date) {
            cell.setCellValue(date);
        } else if (value instanceof Boolean bool) {
            cell.setCellValue(bool);
        } else {
            cell.setCellValue(String.valueOf(value));
        }
    }
}
