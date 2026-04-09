package org.dromara.docman.application.service;

import lombok.RequiredArgsConstructor;
import org.dromara.common.core.application.QueryApplicationService;
import org.dromara.docman.domain.vo.DocWorkflowTemplateVo;
import org.dromara.docman.service.IDocWorkflowTemplateService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DocWorkflowTemplateQueryApplicationService implements QueryApplicationService {

    private final IDocWorkflowTemplateService workflowTemplateService;

    public List<DocWorkflowTemplateVo> listByProjectType(String projectTypeCode) {
        return workflowTemplateService.listByProjectType(projectTypeCode);
    }

    public DocWorkflowTemplateVo queryById(Long id) {
        return workflowTemplateService.queryById(id);
    }
}
