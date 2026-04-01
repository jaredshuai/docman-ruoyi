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

    /**
     * 创建项目。
     *
     * @param bo 项目参数
     * @return 项目ID
     */
    public Long create(DocProjectBo bo) {
        return projectService.insertProject(bo);
    }

    /**
     * 更新项目。
     *
     * @param bo 项目参数
     */
    public void update(DocProjectBo bo) {
        projectService.updateProject(bo);
    }

    /**
     * 批量删除项目。
     *
     * @param ids 项目ID列表
     */
    public void delete(List<Long> ids) {
        projectService.deleteByIds(ids);
    }
}
