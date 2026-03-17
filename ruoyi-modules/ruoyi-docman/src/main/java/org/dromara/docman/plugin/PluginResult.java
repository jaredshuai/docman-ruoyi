package org.dromara.docman.plugin;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class PluginResult {

    private boolean success;
    private String errorMessage;
    private List<GeneratedFile> generatedFiles;

    @Data
    @Builder
    public static class GeneratedFile {
        private String fileName;
        private String nasPath;
        private Long ossId;
    }

    public static PluginResult ok(List<GeneratedFile> files) {
        return PluginResult.builder().success(true).generatedFiles(files).build();
    }

    public static PluginResult ok() {
        return PluginResult.builder().success(true).build();
    }

    public static PluginResult fail(String message) {
        return PluginResult.builder().success(false).errorMessage(message).build();
    }
}
