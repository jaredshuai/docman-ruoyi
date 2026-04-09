package org.dromara.docman.application.service;

import lombok.RequiredArgsConstructor;
import org.dromara.common.core.application.CommandApplicationService;
import org.dromara.docman.domain.bo.DocProjectTypeBo;
import org.dromara.docman.service.IDocProjectTypeService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DocProjectTypeApplicationService implements CommandApplicationService {

    private final IDocProjectTypeService projectTypeService;

    public Long save(DocProjectTypeBo bo) {
        return projectTypeService.save(bo);
    }

    public void delete(List<Long> ids) {
        projectTypeService.deleteByIds(ids);
    }
}
