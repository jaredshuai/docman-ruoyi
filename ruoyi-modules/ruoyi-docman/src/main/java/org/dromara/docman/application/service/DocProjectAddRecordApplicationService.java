package org.dromara.docman.application.service;

import lombok.RequiredArgsConstructor;
import org.dromara.common.core.application.CommandApplicationService;
import org.dromara.docman.domain.bo.DocProjectAddRecordBo;
import org.dromara.docman.service.IDocProjectAddRecordService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DocProjectAddRecordApplicationService implements CommandApplicationService {

    private final IDocProjectAddRecordService addRecordService;

    /**
     * 保存工作量记录
     *
     * @param bo 工作量记录参数
     * @return 工作量记录 ID
     */
    public Long save(DocProjectAddRecordBo bo) {
        return addRecordService.save(bo);
    }

    /**
     * 批量删除工作量记录
     *
     * @param ids 工作量记录 ID 列表
     */
    public void delete(List<Long> ids) {
        addRecordService.deleteByIds(ids);
    }

    /**
     * 删除项目的所有工作量记录
     *
     * @param projectId 项目 ID
     */
    public void deleteByProjectId(Long projectId) {
        addRecordService.deleteByProjectId(projectId);
    }
}
