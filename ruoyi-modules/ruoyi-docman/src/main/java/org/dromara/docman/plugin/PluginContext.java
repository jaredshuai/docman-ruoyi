package org.dromara.docman.plugin;

import lombok.Builder;
import lombok.Data;
import org.dromara.docman.context.NodeContextReader;

import java.util.Map;
import java.util.function.BiConsumer;

@Data
@Builder
public class PluginContext {
    private Long projectId;
    private String projectName;
    private Long processInstanceId;
    private String nodeCode;
    private NodeContextReader contextReader;
    private BiConsumer<String, Object> processWriter;
    private BiConsumer<String, Object> nodeWriter;
    private BiConsumer<String, Object> factWriter;
    private BiConsumer<String, String> contentWriter;
    private Map<String, Object> pluginConfig;
    private String nasBasePath;
    private String archiveFolderName;
}
