package org.dromara.docman.application.service;

import lombok.RequiredArgsConstructor;
import org.dromara.common.core.application.QueryApplicationService;
import org.dromara.docman.application.assembler.DocProjectMemberAssembler;
import org.dromara.docman.domain.vo.DocProjectMemberVo;
import org.dromara.docman.service.IDocProjectMemberService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DocProjectMemberQueryApplicationService implements QueryApplicationService {

    private final IDocProjectMemberService projectMemberService;
    private final DocProjectMemberAssembler projectMemberAssembler;

    /**
     * 查询项目成员列表。
     *
     * @param projectId 项目ID
     * @return 成员列表
     */
    public List<DocProjectMemberVo> list(Long projectId) {
        return projectMemberAssembler.toVoList(projectMemberService.listByProjectId(projectId));
    }
}
