package org.dromara.docman.plugin;

import java.util.List;

/**
 * 文档插件接口
 */
public interface DocumentPlugin {

    String getPluginId();

    String getPluginName();

    PluginType getPluginType();

    List<FieldDef> getInputFields();

    List<FieldDef> getOutputFields();

    PluginResult execute(PluginContext context);
}
