package org.dromara.docman.application.service;

import lombok.RequiredArgsConstructor;
import org.dromara.common.core.application.CommandApplicationService;
import org.dromara.docman.service.IDocProcessService;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DocProcessApplicationService implements CommandApplicationService {

    private final IDocProcessService processService;

    public void bind(Long projectId, Long definitionId) {
        processService.bindProcess(projectId, definitionId);
    }

    public Long start(Long projectId) {
        return processService.startProcess(projectId);
    }
}
