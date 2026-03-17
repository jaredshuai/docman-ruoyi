package org.dromara.docman.plugin.runtime;

import lombok.Builder;
import lombok.Data;
import org.dromara.docman.plugin.DocumentPlugin;
import org.dromara.docman.plugin.PluginContext;

@Data
@Builder
public class PluginExecutionRequest {

    private DocumentPlugin plugin;

    private PluginContext context;
}
