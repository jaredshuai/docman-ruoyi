package org.dromara.docman.application.authz;

/**
 * 仅由本地会话中的 canonical issuer/sub 构建的可信主体。
 * <p>
 * 不接受请求参数、Cookie、普通 Header、username、userId 或工号作为来源。
 */
public record CanonicalPrincipal(String issuer, String subject) {

    public CanonicalPrincipal {
        if (issuer == null || issuer.isBlank()) {
            throw new IllegalArgumentException("canonical issuer must not be blank");
        }
        if (subject == null || subject.isBlank()) {
            throw new IllegalArgumentException("canonical subject must not be blank");
        }
    }
}
