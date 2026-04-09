package org.dromara.docman.service;

import org.dromara.docman.domain.bo.DocWorkflowTemplateBo;
import org.dromara.docman.domain.vo.DocWorkflowTemplateVo;

import java.util.List;

public interface IDocWorkflowTemplateService {

    List<DocWorkflowTemplateVo> listByProjectType(String projectTypeCode);

    DocWorkflowTemplateVo queryById(Long id);

    Long save(DocWorkflowTemplateBo bo);

    void deleteByIds(List<Long> ids);
}
