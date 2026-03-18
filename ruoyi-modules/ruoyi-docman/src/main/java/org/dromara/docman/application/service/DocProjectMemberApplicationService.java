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

    public void add(Long projectId, DocProjectMemberBo bo) {
        bo.setProjectId(projectId);
        projectMemberService.addMember(bo);
    }

    public void remove(Long projectId, Long userId) {
        projectMemberService.removeMember(projectId, userId);
    }
}
