package org.dromara.docman.plugin.runtime;

import lombok.Builder;
import lombok.Data;
import org.dromara.docman.plugin.PluginResult;

@Data
@Builder
public class PluginExecutionResult {

    private String pluginId;

    private long costMs;

    private PluginResult result;
}
