package org.dromara.docman.application.service;

import lombok.RequiredArgsConstructor;
import org.dromara.common.core.application.CommandApplicationService;
import org.dromara.docman.domain.bo.DocProjectBo;
import org.dromara.docman.service.IDocProjectService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DocProjectApplicationService implements CommandApplicationService {

    private final IDocProjectService projectService;

    public Long create(DocProjectBo bo) {
        return projectService.insertProject(bo);
    }

    public void update(DocProjectBo bo) {
        projectService.updateProject(bo);
    }

    public void delete(List<Long> ids) {
        projectService.deleteByIds(ids);
    }
}
