package org.dromara.docman.application.assembler;

import org.dromara.docman.domain.vo.DocPluginInfoVo;
import org.dromara.docman.plugin.DocumentPlugin;
import org.springframework.stereotype.Component;

@Component
public class DocPluginAssembler {

    public DocPluginInfoVo toInfoVo(DocumentPlugin plugin) {
        DocPluginInfoVo vo = new DocPluginInfoVo();
        vo.setPluginId(plugin.getPluginId());
        vo.setPluginName(plugin.getPluginName());
        vo.setPluginType(plugin.getPluginType().getCode());
        vo.setInputFields(plugin.getInputFields());
        vo.setOutputFields(plugin.getOutputFields());
        return vo;
    }
}
