package org.dromara.docman.application.service;

import lombok.RequiredArgsConstructor;
import org.dromara.common.core.application.CommandApplicationService;
import org.dromara.docman.domain.bo.DocWorkflowTemplateBo;
import org.dromara.docman.service.IDocWorkflowTemplateService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DocWorkflowTemplateApplicationService implements CommandApplicationService {

    private final IDocWorkflowTemplateService workflowTemplateService;

    public Long save(DocWorkflowTemplateBo bo) {
        return workflowTemplateService.save(bo);
    }

    public void delete(List<Long> ids) {
        workflowTemplateService.deleteByIds(ids);
    }
}
