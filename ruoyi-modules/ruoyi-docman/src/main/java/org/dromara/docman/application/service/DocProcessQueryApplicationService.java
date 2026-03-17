package org.dromara.docman.application.service;

import lombok.RequiredArgsConstructor;
import org.dromara.common.core.application.QueryApplicationService;
import org.dromara.docman.application.assembler.DocProcessAssembler;
import org.dromara.docman.domain.vo.DocProcessConfigVo;
import org.dromara.docman.service.IDocProcessService;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DocProcessQueryApplicationService implements QueryApplicationService {

    private final IDocProcessService processService;
    private final DocProcessAssembler processAssembler;

    public DocProcessConfigVo getConfig(Long projectId) {
        return processAssembler.toVo(processService.getByProjectId(projectId));
    }
}
