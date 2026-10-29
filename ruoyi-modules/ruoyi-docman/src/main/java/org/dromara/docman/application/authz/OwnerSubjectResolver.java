package org.dromara.docman.application.authz;

import java.util.Optional;

/**
 * 从本地项目负责人反查其 canonical 外部身份。
 * <p>
 * 只允许使用 {@code sys_social.auth_id} 中的 OIDC 命名空间，解析结果必须唯一且与当前租户一致。
 * 任何不确定的场景都返回 empty，调用方必须 fail closed。
 */
public interface OwnerSubjectResolver {

    /**
     * @param ownerId  本地负责人用户 ID，不得直接作为跨系统授权主体
     * @param tenantId 期望的本地租户标识
     * @return 唯一解析出的 canonical issuer/sub；无法唯一确定时为空
     */
    Optional<CanonicalPrincipal> resolve(Long ownerId, String tenantId);
}
