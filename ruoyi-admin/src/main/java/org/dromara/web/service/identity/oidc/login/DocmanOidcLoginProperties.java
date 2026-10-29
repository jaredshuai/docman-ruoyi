package org.dromara.web.service.identity.oidc.login;

import org.dromara.web.service.identity.oidc.DocmanOidcProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Casdoor authorization-code + PKCE 登录的显式配置合同。
 * <p>
 * 全部值来自环境变量或 Spring 配置，不提供任何默认 issuer、client id、client secret 或身份。
 * client secret 缺失时 {@link #isLoginConfigured()} 返回 {@code false}，调用方必须 fail closed。
 *
 * @param enabled           是否启用 OIDC 登录
 * @param issuer            期望 ID Token 的精确原始 issuer URL，仅用于 JWT 校验
 * @param canonicalIssuer   授权中心/已有消费者使用的 canonical issuer key，必须显式配置，
 *                          禁止从 issuer URL、用户名或请求推导，也不提供默认值
 * @param jwksUri           JWKS 地址，用于验签
 * @param audience          期望 audience，即 Casdoor client_id
 * @param clientId          可选的显式 client_id；为空时回退到 audience
 * @param clientSecret      运行时 client secret，只允许来自环境变量/Secret，禁止写入仓库
 * @param authorizationUri  Casdoor authorization endpoint
 * @param tokenUri          Casdoor token endpoint
 * @param redirectUri       本应用回调地址
 * @param tenantId          绑定与授权使用的本地租户标识
 * @param scope             授权 scope，例如 openid
 * @param stateTtl          state/nonce/verifier 服务端临时存储 TTL
 * @param connectTimeout    token endpoint 连接超时
 * @param requestTimeout    token endpoint 请求超时
 */
@ConfigurationProperties(prefix = "docman.oidc")
public record DocmanOidcLoginProperties(
    boolean enabled,
    String issuer,
    String canonicalIssuer,
    String jwksUri,
    String audience,
    String clientId,
    String clientSecret,
    String authorizationUri,
    String tokenUri,
    String redirectUri,
    String tenantId,
    String scope,
    Duration stateTtl,
    Duration connectTimeout,
    Duration requestTimeout
) {

    private static final Duration DEFAULT_STATE_TTL = Duration.ofMinutes(5);
    private static final Duration MAX_STATE_TTL = Duration.ofMinutes(10);
    private static final Duration MIN_STATE_TTL = Duration.ofSeconds(30);
    private static final Duration DEFAULT_CONNECT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration DEFAULT_REQUEST_TIMEOUT = Duration.ofSeconds(10);

    public DocmanOidcLoginProperties {
        stateTtl = normalize(stateTtl, DEFAULT_STATE_TTL, MIN_STATE_TTL, MAX_STATE_TTL);
        connectTimeout = normalize(connectTimeout, DEFAULT_CONNECT_TIMEOUT, Duration.ofSeconds(1), Duration.ofSeconds(30));
        requestTimeout = normalize(requestTimeout, DEFAULT_REQUEST_TIMEOUT, Duration.ofSeconds(1), Duration.ofSeconds(60));
    }

    /**
     * 完整登录链路是否已显式配置；任一必需值为空白都视为未配置。
     */
    public boolean isLoginConfigured() {
        return enabled
            && notBlank(issuer)
            && notBlank(canonicalIssuer)
            && notBlank(jwksUri)
            && notBlank(audience)
            && notBlank(clientSecret)
            && notBlank(authorizationUri)
            && notBlank(tokenUri)
            && notBlank(redirectUri)
            && notBlank(tenantId)
            && notBlank(scope);
    }

    /**
     * Casdoor client_id 与 audience 为同一概念；显式 clientId 优先，其次使用 audience。
     */
    public String effectiveClientId() {
        return notBlank(clientId) ? clientId : audience;
    }

    /**
     * 转换为既有 ID Token 验证器使用的配置；client secret 不进入验证器。
     */
    public DocmanOidcProperties toVerifierProperties() {
        return new DocmanOidcProperties(enabled, issuer, jwksUri, audience, tenantId);
    }

    /**
     * 避免 client secret 通过日志或异常信息泄漏。
     */
    @Override
    public String toString() {
        return "DocmanOidcLoginProperties["
            + "enabled=" + enabled
            + ", issuer=" + issuer
            + ", canonicalIssuer=" + canonicalIssuer
            + ", jwksUri=" + jwksUri
            + ", audience=" + audience
            + ", clientId=" + clientId
            + ", clientSecret=<redacted>"
            + ", authorizationUri=" + authorizationUri
            + ", tokenUri=" + tokenUri
            + ", redirectUri=" + redirectUri
            + ", tenantId=" + tenantId
            + ", scope=" + scope
            + ", stateTtl=" + stateTtl
            + ", connectTimeout=" + connectTimeout
            + ", requestTimeout=" + requestTimeout
            + ']';
    }

    private static Duration normalize(Duration value, Duration fallback, Duration min, Duration max) {
        Duration candidate = value == null ? fallback : value;
        if (candidate.compareTo(min) < 0) {
            return min;
        }
        if (candidate.compareTo(max) > 0) {
            return max;
        }
        return candidate;
    }

    private static boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }
}
