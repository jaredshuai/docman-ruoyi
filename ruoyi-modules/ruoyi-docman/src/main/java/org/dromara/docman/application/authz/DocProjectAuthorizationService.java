package org.dromara.docman.application.authz;

import com.jxccs.authz.AccountStatus;
import com.jxccs.authz.AuthorizationClient;
import com.jxccs.authz.AuthorizationClientException;
import com.jxccs.authz.ClientFailure;
import com.jxccs.authz.Decision;
import com.jxccs.authz.Principal;
import com.jxccs.authz.ResourceRef;
import lombok.RequiredArgsConstructor;
import org.dromara.docman.domain.vo.DocProjectVo;
import org.dromara.docman.infrastructure.authz.DocmanAuthorizationClientProvider;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * 项目详情读取的最小 PEP。
 * <p>
 * 只使用本地会话中的 canonical issuer/sub 作为请求主体；资源侧仅承诺项目负责人的
 * canonical ownerSubject，不发送本地 userId、成员、角色或项目状态等未冻结属性。
 * 无可信会话主体返回 401；负责人 canonical 绑定缺失或无法唯一确定返回 403；
 * 授权中心不可用或配置缺失返回 503。
 */
@Service
@RequiredArgsConstructor
public class DocProjectAuthorizationService {

    static final String PERMISSION = "docman:project:query";
    static final String RESOURCE_TYPE = "project";
    static final String OWNER_SUBJECT = "ownerSubject";

    private final CanonicalPrincipalProvider principalProvider;
    private final OwnerSubjectResolver ownerSubjectResolver;
    private final DocmanAuthorizationClientProvider clientProvider;

    /**
     * 校验当前 canonical 主体是否可读取指定项目；通过时保持原读取结果。
     */
    public void checkRead(Long projectId, DocProjectVo project) {
        CanonicalPrincipal canonicalPrincipal = principalProvider.current()
            .orElseThrow(() -> DocProjectAuthorizationException.unauthenticated(
                "trusted canonical principal is required"));

        CanonicalPrincipal ownerSubject = resolveOwnerSubject(project)
            .orElseThrow(() -> DocProjectAuthorizationException.forbidden(
                "project owner canonical binding is unavailable"));

        AuthorizationClient client = clientProvider.client()
            .orElseThrow(() -> DocProjectAuthorizationException.unavailable(
                "authorization service is not configured or unavailable"));

        Decision decision;
        try {
            Principal principal = new Principal(
                canonicalPrincipal.issuer(),
                canonicalPrincipal.subject(),
                AccountStatus.ACTIVE
            );
            ResourceRef resource = new ResourceRef(
                RESOURCE_TYPE,
                String.valueOf(projectId),
                ownerSubjectAttributes(ownerSubject)
            );
            decision = client.check(principal, PERMISSION, resource, UUID.randomUUID().toString());
        } catch (AuthorizationClientException ex) {
            throw mapClientFailure(ex);
        } catch (RuntimeException ex) {
            throw DocProjectAuthorizationException.unavailable("authorization service is unavailable");
        }

        if (!decision.allowed()) {
            throw DocProjectAuthorizationException.forbidden("project access is not allowed");
        }
    }

    private Optional<CanonicalPrincipal> resolveOwnerSubject(DocProjectVo project) {
        if (project == null || project.getOwnerId() == null) {
            return Optional.empty();
        }
        Optional<String> tenantId = principalProvider.currentTenantId();
        if (tenantId.isEmpty()) {
            return Optional.empty();
        }
        try {
            return ownerSubjectResolver.resolve(project.getOwnerId(), tenantId.get());
        } catch (RuntimeException ex) {
            return Optional.empty();
        }
    }

    private static DocProjectAuthorizationException mapClientFailure(AuthorizationClientException ex) {
        ClientFailure failure = ex.failure();
        if (failure != null && failure.businessStatus() == 401) {
            return DocProjectAuthorizationException.unauthenticated("authorization service did not accept the principal");
        }
        return DocProjectAuthorizationException.unavailable("authorization service is unavailable");
    }

    private static Map<String, Object> ownerSubjectAttributes(CanonicalPrincipal ownerSubject) {
        return Map.of(OWNER_SUBJECT, Map.of(
            "issuer", ownerSubject.issuer(),
            "sub", ownerSubject.subject()
        ));
    }
}
