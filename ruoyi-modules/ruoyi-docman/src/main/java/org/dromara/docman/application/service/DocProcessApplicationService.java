package org.dromara.docman.application.service;

import lombok.RequiredArgsConstructor;
import org.dromara.common.core.application.CommandApplicationService;
import org.dromara.docman.service.IDocProcessService;
import org.dromara.warm.flow.core.enums.PublishStatus;
import org.dromara.warm.flow.core.service.DefService;
import org.dromara.warm.flow.orm.entity.FlowDefinition;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DocProcessApplicationService implements CommandApplicationService {

    private final IDocProcessService processService;
    private final DefService definitionService;

    public void bind(Long projectId, Long definitionId) {
        processService.bindProcess(projectId, definitionId);
    }

    public Long start(Long projectId) {
        return processService.startProcess(projectId);
    }

    public List<Map<String, Object>> listDefinitions() {
        FlowDefinition query = new FlowDefinition();
        query.setIsPublish(PublishStatus.PUBLISHED.getKey());
        List<org.dromara.warm.flow.core.entity.Definition> definitions = definitionService.list(query);
        return definitions.stream()
            .map(d -> Map.of("id", (Object) d.getId(), "name", (Object) d.getFlowName()))
            .collect(Collectors.toList());
    }
}
