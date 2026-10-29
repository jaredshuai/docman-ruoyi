package org.dromara.web.service.identity.oidc;

import java.util.Optional;

/**
 * 已完成签名、时间、issuer、audience/azp 与 nonce 校验的 ID Token 身份声明。
 * <p>
 * {@code issuer} 是 ID Token 中经过严格校验的原始 issuer URL；它不是授权中心使用的
 * canonical issuer key。调用方必须显式配置 canonical issuer 并在校验成功后完成转换。
 *
 * @param issuer   ID Token 的原始 {@code iss}，已与配置的原始 issuer URL 精确匹配
 * @param subject  ID Token 的 {@code sub}
 * @param tenantId 本地租户标识
 */
public record VerifiedOidcToken(String issuer, String subject, String tenantId) {

    /**
     * 构造验证结果；任一输入为空白时明确拒绝。
     */
    public static Optional<VerifiedOidcToken> of(String issuer, String subject, String tenantId) {
        if (isBlank(issuer) || isBlank(subject) || isBlank(tenantId)) {
            return Optional.empty();
        }
        return Optional.of(new VerifiedOidcToken(issuer, subject, tenantId));
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
