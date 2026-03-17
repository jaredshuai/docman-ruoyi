package org.dromara.docman.service;

import org.dromara.docman.domain.enums.DocProjectAction;
import org.dromara.docman.domain.enums.DocProjectRole;

import java.util.List;

public interface IDocProjectAccessService {

    List<Long> listAccessibleProjectIds(Long userId);

    DocProjectRole getCurrentRole(Long projectId);

    void assertAction(Long projectId, DocProjectAction action);
}
