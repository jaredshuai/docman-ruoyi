package org.dromara.docman.domain.service;

import org.dromara.common.core.permission.RoleActionPolicy;
import org.dromara.docman.domain.enums.DocProjectAction;
import org.dromara.docman.domain.enums.DocProjectRole;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.Map;

/**
 * 项目角色权限策略。继承框架通用 {@link RoleActionPolicy}。
 */
@Component
public class DocProjectPermissionPolicy extends RoleActionPolicy<DocProjectRole, DocProjectAction> {

    public DocProjectPermissionPolicy() {
        super(Map.of(
            DocProjectRole.OWNER, EnumSet.allOf(DocProjectAction.class),
            DocProjectRole.EDITOR, EnumSet.of(
                DocProjectAction.VIEW_PROJECT,
                DocProjectAction.EDIT_PROJECT,
                DocProjectAction.VIEW_DOCUMENT,
                DocProjectAction.UPLOAD_DOCUMENT,
                DocProjectAction.VIEW_PROCESS,
                DocProjectAction.VIEW_ARCHIVE
            ),
            DocProjectRole.VIEWER, EnumSet.of(
                DocProjectAction.VIEW_PROJECT,
                DocProjectAction.VIEW_DOCUMENT,
                DocProjectAction.VIEW_PROCESS,
                DocProjectAction.VIEW_ARCHIVE
            )
        ));
    }
}
