package org.dromara.web.service.identity.oidc;

import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Optional;

/**
 * 标准 Casdoor OIDC ID Token 验证器。
 * <p>
 * 仅使用 Spring Security JOSE 的 {@link NimbusJwtDecoder} 与 {@link JwtValidators} 完成签名/JWKS、
 * issuer、时间与 audience 校验，不自行解析或校验 JWT 的 base64/签名，也不访问任何外部 live 环境。
 * <p>
 * 输出只有已完成校验的 {@link VerifiedOidcToken}（原始 issuer URL + sub + tenantId）；任何配置缺失、
 * 空白输入、无效/过期 token、issuer 或 audience 不匹配都返回 {@link Optional#empty()}，绝不生成
 * Principal，也绝不把 username/userId/employeeId 当作 subject，且不调用任何绑定服务。
 * <p>
 * 原始 issuer URL 仅用于 JWT 校验，授权中心使用的 canonical issuer key 由登录编排在校验成功后显式转换。
 */
public final class CasdoorOidcTokenVerifier {

    private final DocmanOidcProperties properties;
    private final JwtDecoder jwtDecoder;

    /**
     * 生产构造：按显式配置从 JWKS 地址构建解码器。未完整配置时不构建解码器，后续一律 fail closed。
     */
    public CasdoorOidcTokenVerifier(DocmanOidcProperties properties) {
        this(properties, buildDecoder(properties));
    }

    /**
     * 可注入解码器的构造，便于完全离线的单元测试；校验器仍由本类统一装配。
     */
    CasdoorOidcTokenVerifier(DocmanOidcProperties properties, NimbusJwtDecoder injectedDecoder) {
        this.properties = properties;
        this.jwtDecoder = applyValidators(properties, injectedDecoder);
    }

    /**
     * 校验 ID Token 并返回验证结果，不校验 nonce。
     *
     * @param idToken 待校验的 ID Token 原始串
     * @return 校验通过时返回原始 issuer/sub/tenantId；否则 empty
     */
    public Optional<VerifiedOidcToken> verify(String idToken) {
        return verify(idToken, null);
    }

    /**
     * 校验 ID Token 并返回验证结果。
     *
     * @param idToken       待校验的 ID Token 原始串
     * @param expectedNonce 授权请求生成的 nonce；传入时要求 ID Token 携带完全一致的 nonce
     * @return 校验通过时返回原始 issuer/sub/tenantId；否则 empty
     */
    public Optional<VerifiedOidcToken> verify(String idToken, String expectedNonce) {
        if (properties == null || !properties.isConfigured() || jwtDecoder == null) {
            return Optional.empty();
        }
        if (idToken == null || idToken.isBlank()) {
            return Optional.empty();
        }
        try {
            Jwt jwt = jwtDecoder.decode(idToken.trim());
            String issuer = jwt.getClaimAsString(JwtClaimNames.ISS);
            if (issuer == null || !issuer.equals(properties.issuer())) {
                return Optional.empty();
            }
            if (!nonceMatches(jwt, expectedNonce)) {
                return Optional.empty();
            }
            String subject = jwt.getClaimAsString(JwtClaimNames.SUB);
            return VerifiedOidcToken.of(issuer, subject, properties.tenantId());
        } catch (JwtException | IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

    private static boolean nonceMatches(Jwt jwt, String expectedNonce) {
        if (expectedNonce == null) {
            return true;
        }
        if (expectedNonce.isBlank()) {
            return false;
        }
        String nonce = jwt.getClaimAsString("nonce");
        if (nonce == null) {
            return false;
        }
        return MessageDigest.isEqual(
            nonce.getBytes(StandardCharsets.UTF_8),
            expectedNonce.getBytes(StandardCharsets.UTF_8)
        );
    }

    private static NimbusJwtDecoder buildDecoder(DocmanOidcProperties properties) {
        if (properties == null || !properties.isConfigured()) {
            return null;
        }
        return NimbusJwtDecoder.withJwkSetUri(properties.jwksUri()).build();
    }

    private static JwtDecoder applyValidators(DocmanOidcProperties properties, NimbusJwtDecoder decoder) {
        if (properties == null || !properties.isConfigured() || decoder == null) {
            return null;
        }
        OAuth2TokenValidator<Jwt> validator = new DelegatingOAuth2TokenValidator<>(
            JwtValidators.createDefaultWithIssuer(properties.issuer()),
            new AudienceValidator(properties.audience())
        );
        decoder.setJwtValidator(validator);
        return decoder;
    }
}
