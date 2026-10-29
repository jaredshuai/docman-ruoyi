package org.dromara.docman.application.authz;

import java.util.Optional;

/**
 * 从现有本地会话读取可信 canonical 主体。
 */
public interface CanonicalPrincipalProvider {

    Optional<CanonicalPrincipal> current();

    /**
     * 当前本地会话的租户标识，用于约束负责人绑定的租户范围。
     */
    Optional<String> currentTenantId();
}
