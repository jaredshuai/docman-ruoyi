package org.dromara.docman.application.service;

import lombok.RequiredArgsConstructor;
import org.dromara.common.core.application.QueryApplicationService;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.docman.domain.vo.DocDocumentRecordVo;
import org.dromara.docman.service.IDocDocumentRecordService;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DocDocumentQueryApplicationService implements QueryApplicationService {

    private final IDocDocumentRecordService documentRecordService;

    /**
     * 分页查询项目文档。
     *
     * @param projectId 项目ID
     * @param pageQuery 分页参数
     * @return 文档分页结果
     */
    public TableDataInfo<DocDocumentRecordVo> list(Long projectId, PageQuery pageQuery) {
        return documentRecordService.queryPageList(projectId, pageQuery);
    }

    /**
     * 查询单个文档详情。
     *
     * @param id 文档记录ID
     * @return 文档详情
     */
    public DocDocumentRecordVo getById(Long id) {
        return documentRecordService.queryById(id);
    }
}
