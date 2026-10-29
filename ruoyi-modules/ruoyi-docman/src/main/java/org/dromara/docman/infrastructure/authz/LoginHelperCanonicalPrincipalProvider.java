package org.dromara.docman.infrastructure.authz;

import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.docman.application.authz.CanonicalPrincipal;
import org.dromara.docman.application.authz.CanonicalPrincipalProvider;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * 只从 Sa-Token 会话 extra 中的 canonical issuer/sub 读取主体。
 * <p>
 * username、userId、工号、请求参数、Cookie 与普通 Header 都不参与 Principal。
 */
@Component
public class LoginHelperCanonicalPrincipalProvider implements CanonicalPrincipalProvider {

    @Override
    public Optional<CanonicalPrincipal> current() {
        try {
            String issuer = LoginHelper.getCanonicalIssuer();
            String subject = LoginHelper.getCanonicalSubject();
            if (issuer == null || issuer.isBlank() || subject == null || subject.isBlank()) {
                return Optional.empty();
            }
            return Optional.of(new CanonicalPrincipal(issuer, subject));
        } catch (RuntimeException ex) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<String> currentTenantId() {
        try {
            String tenantId = LoginHelper.getTenantId();
            if (tenantId == null || tenantId.isBlank()) {
                return Optional.empty();
            }
            return Optional.of(tenantId);
        } catch (RuntimeException ex) {
            return Optional.empty();
        }
    }
}
