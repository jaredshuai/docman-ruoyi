package org.dromara.docman.application.service;

import lombok.RequiredArgsConstructor;
import org.dromara.common.core.application.CommandApplicationService;
import org.dromara.docman.application.assembler.DocProcessAssembler;
import org.dromara.docman.domain.vo.DocProcessConfigVo;
import org.dromara.docman.service.IDocProcessService;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DocProcessApplicationService implements CommandApplicationService {

    private final IDocProcessService processService;
    private final DocProcessAssembler processAssembler;

    public void bind(Long projectId, Long definitionId) {
        processService.bindProcess(projectId, definitionId);
    }

    public Long start(Long projectId) {
        return processService.startProcess(projectId);
    }

    public DocProcessConfigVo getConfig(Long projectId) {
        return processAssembler.toVo(processService.getByProjectId(projectId));
    }
}
