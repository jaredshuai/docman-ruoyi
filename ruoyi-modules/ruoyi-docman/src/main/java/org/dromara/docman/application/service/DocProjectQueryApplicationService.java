package org.dromara.docman.application.service;

import lombok.RequiredArgsConstructor;
import org.dromara.common.core.application.QueryApplicationService;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.docman.domain.bo.DocProjectBo;
import org.dromara.docman.domain.vo.DocProjectVo;
import org.dromara.docman.service.IDocProjectService;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DocProjectQueryApplicationService implements QueryApplicationService {

    private final IDocProjectService projectService;

    public TableDataInfo<DocProjectVo> list(DocProjectBo bo, PageQuery pageQuery) {
        return projectService.queryPageList(bo, pageQuery);
    }

    public DocProjectVo getById(Long id) {
        return projectService.queryById(id);
    }
}
