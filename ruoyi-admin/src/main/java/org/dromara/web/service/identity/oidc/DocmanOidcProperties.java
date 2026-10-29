package org.dromara.web.service.identity.oidc;

/**
 * 显式 OIDC 配置合同。
 * <p>
 * 所有值必须由部署方显式提供，本类型不定义任何默认 issuer、jwks、audience 或 client secret。
 * 任一必需值为空白时，{@link #isConfigured()} 返回 {@code false}，调用方必须 fail closed。
 *
 * @param enabled  是否启用 direct Casdoor OIDC；仅 {@code true} 时才可能被视为已配置
 * @param issuer   期望的精确 issuer 字符串，必须与 ID Token 的 {@code iss} 完全一致
 * @param jwksUri  Casdoor 提供的 JWKS 地址，用于验签
 * @param audience 期望的 audience，即 Casdoor client_id，对应 ID Token 的 {@code aud}
 * @param tenantId 验签成功后写入 {@link VerifiedOidcToken} 的本地租户标识
 */
public record DocmanOidcProperties(
    boolean enabled,
    String issuer,
    String jwksUri,
    String audience,
    String tenantId
) {

    /**
     * 配置是否完整可用；缺省、空白或未启用均视为未配置。
     */
    public boolean isConfigured() {
        return enabled
            && notBlank(issuer)
            && notBlank(jwksUri)
            && notBlank(audience)
            && notBlank(tenantId);
    }

    /**
     * Casdoor client_id 与 audience 为同一概念，便于配置侧命名一致。
     */
    public String clientId() {
        return audience;
    }

    private static boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }
}
