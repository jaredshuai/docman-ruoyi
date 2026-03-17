package org.dromara.docman.plugin.runtime;

import lombok.extern.slf4j.Slf4j;
import org.dromara.common.json.utils.JsonUtils;
import org.dromara.docman.application.port.out.PluginExecutionLogPort;
import org.dromara.docman.domain.entity.DocPluginExecutionLog;
import org.dromara.docman.domain.enums.DocPluginExecutionStatus;
import org.dromara.docman.plugin.PluginResult;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class PluginExecutor {

    private final PluginExecutionLogPort pluginExecutionLogPort;

    public PluginExecutor(PluginExecutionLogPort pluginExecutionLogPort) {
        this.pluginExecutionLogPort = pluginExecutionLogPort;
    }

    public PluginExecutionResult execute(PluginExecutionRequest request) {
        long start = System.currentTimeMillis();
        PluginExecutionResult executionResult;
        try {
            PluginResult result = request.getPlugin().execute(request.getContext());
            executionResult = PluginExecutionResult.builder()
                .pluginId(request.getPlugin().getPluginId())
                .costMs(System.currentTimeMillis() - start)
                .result(result)
                .build();
        } catch (Exception e) {
            log.error("插件执行异常: {}", request.getPlugin().getPluginId(), e);
            executionResult = PluginExecutionResult.builder()
                .pluginId(request.getPlugin().getPluginId())
                .costMs(System.currentTimeMillis() - start)
                .result(PluginResult.fail("插件执行异常: " + e.getMessage()))
                .build();
        }
        persistExecutionLog(request, executionResult);
        return executionResult;
    }

    private void persistExecutionLog(PluginExecutionRequest request, PluginExecutionResult executionResult) {
        try {
            DocPluginExecutionLog executionLog = new DocPluginExecutionLog();
            executionLog.setProjectId(request.getContext().getProjectId());
            executionLog.setProcessInstanceId(request.getContext().getProcessInstanceId());
            executionLog.setNodeCode(request.getContext().getNodeCode());
            executionLog.setPluginId(request.getPlugin().getPluginId());
            executionLog.setPluginName(request.getPlugin().getPluginName());
            executionLog.setStatus(executionResult.getResult().isSuccess()
                ? DocPluginExecutionStatus.SUCCESS.getCode()
                : DocPluginExecutionStatus.FAILED.getCode());
            executionLog.setCostMs(executionResult.getCostMs());
            executionLog.setGeneratedFileCount(getGeneratedFileCount(executionResult.getResult()));
            executionLog.setErrorMessage(executionResult.getResult().getErrorMessage());
            executionLog.setRequestSnapshot(toJsonString(buildRequestSnapshot(request)));
            executionLog.setResultSnapshot(toJsonString(buildResultSnapshot(executionResult.getResult())));
            pluginExecutionLogPort.save(executionLog);
        } catch (Exception e) {
            log.warn("插件执行日志落库失败: {}", request.getPlugin().getPluginId(), e);
        }
    }

    private Map<String, Object> buildRequestSnapshot(PluginExecutionRequest request) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("projectId", request.getContext().getProjectId());
        snapshot.put("projectName", request.getContext().getProjectName());
        snapshot.put("processInstanceId", request.getContext().getProcessInstanceId());
        snapshot.put("nodeCode", request.getContext().getNodeCode());
        snapshot.put("pluginConfig", request.getContext().getPluginConfig());
        snapshot.put("nasBasePath", request.getContext().getNasBasePath());
        snapshot.put("archiveFolderName", request.getContext().getArchiveFolderName());
        return snapshot;
    }

    private Map<String, Object> buildResultSnapshot(PluginResult result) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("success", result.isSuccess());
        snapshot.put("errorMessage", result.getErrorMessage());
        snapshot.put("generatedFiles", result.getGeneratedFiles());
        return snapshot;
    }

    private Integer getGeneratedFileCount(PluginResult result) {
        List<PluginResult.GeneratedFile> files = result.getGeneratedFiles();
        return files == null ? 0 : files.size();
    }

    private String toJsonString(Object value) {
        try {
            return JsonUtils.toJsonString(value);
        } catch (Exception e) {
            return String.valueOf(value);
        }
    }
}
