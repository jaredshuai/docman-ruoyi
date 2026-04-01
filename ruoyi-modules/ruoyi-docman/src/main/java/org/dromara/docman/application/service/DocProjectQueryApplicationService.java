package org.dromara.docman.application.service;

import lombok.RequiredArgsConstructor;
import org.dromara.common.core.application.QueryApplicationService;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.docman.domain.bo.DocProjectBo;
import org.dromara.docman.domain.vo.DocProjectVo;
import org.dromara.docman.service.IDocProjectService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DocProjectQueryApplicationService implements QueryApplicationService {

    private final IDocProjectService projectService;

    /**
     * 分页查询项目列表。
     *
     * @param bo        筛选参数
     * @param pageQuery 分页参数
     * @return 项目分页结果
     */
    public TableDataInfo<DocProjectVo> list(DocProjectBo bo, PageQuery pageQuery) {
        return projectService.queryPageList(bo, pageQuery);
    }

    /**
     * 查询当前用户项目列表。
     *
     * @param bo 筛选参数
     * @return 当前用户项目
     */
    public List<DocProjectVo> listMy(DocProjectBo bo) {
        return projectService.queryMyList(bo);
    }

    /**
     * 查询项目详情。
     *
     * @param id 项目ID
     * @return 项目详情
     */
    public DocProjectVo getById(Long id) {
        return projectService.queryById(id);
    }
}
