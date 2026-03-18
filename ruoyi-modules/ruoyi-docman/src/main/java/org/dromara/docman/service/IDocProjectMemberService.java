package org.dromara.docman.service;

import org.dromara.docman.domain.bo.DocProjectMemberBo;
import org.dromara.docman.domain.entity.DocProjectMember;

import java.util.List;

public interface IDocProjectMemberService {

    List<DocProjectMember> listByProjectId(Long projectId);

    Long addMember(DocProjectMemberBo bo);

    void removeMember(Long projectId, Long userId);
}
