package org.dromara.docman.application.service;

import lombok.RequiredArgsConstructor;
import org.dromara.common.core.application.QueryApplicationService;
import org.dromara.docman.domain.vo.DocProjectAddRecordVo;
import org.dromara.docman.service.IDocProjectAddRecordService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DocProjectAddRecordQueryApplicationService implements QueryApplicationService {

    private final IDocProjectAddRecordService addRecordService;

    /**
     * 查询项目的工作量记录列表
     *
     * @param projectId 项目 ID
     * @return 工作量记录列表
     */
    public List<DocProjectAddRecordVo> listByProject(Long projectId) {
        return addRecordService.listByProject(projectId);
    }

    /**
     * 查询工作量记录详情
     *
     * @param id 工作量记录 ID
     * @return 工作量记录详情
     */
    public DocProjectAddRecordVo queryById(Long id) {
        return addRecordService.queryById(id);
    }
}
