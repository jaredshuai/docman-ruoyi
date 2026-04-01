package org.dromara.docman.application.service;

import lombok.RequiredArgsConstructor;
import org.dromara.common.core.application.CommandApplicationService;
import org.dromara.docman.domain.bo.DocProjectMemberBo;
import org.dromara.docman.service.IDocProjectMemberService;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DocProjectMemberApplicationService implements CommandApplicationService {

    private final IDocProjectMemberService projectMemberService;

    /**
     * 向项目添加成员。
     *
     * @param projectId 项目ID
     * @param bo        成员参数
     */
    public void add(Long projectId, DocProjectMemberBo bo) {
        bo.setProjectId(projectId);
        projectMemberService.addMember(bo);
    }

    /**
     * 从项目中移除成员。
     *
     * @param projectId 项目ID
     * @param userId    用户ID
     */
    public void remove(Long projectId, Long userId) {
        projectMemberService.removeMember(projectId, userId);
    }
}
