package org.dromara.web.service.identity;

import com.jxccs.authz.ClientConfig;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.dromara.docman.config.DocmanAuthorizationProperties;
import org.dromara.web.service.identity.oidc.login.DocmanOidcLoginProperties;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;

/**
 * 身份链路的启动安全检查。
 * <p>
 * 集成/发布环境发现合成身份提供方、未完成的 OIDC 配置或明显伪造的 issuer/端点时直接拒绝启动；
 * local 环境只允许显式配置的 fixture，不做静默回退。
 */
@Component
@RequiredArgsConstructor
public class IdentitySafetyGuard {

    private static final String LOCAL_PROFILE = "local";
    private static final String PROD_PROFILE = "prod";
    private static final Set<String> SYNTHETIC_TOKENS = Set.of(
        "mock", "fixture", "synthetic", "dummy", "fake", "debug"
    );
    private static final Set<String> SYNTHETIC_AUTHORIZATION_TOKENS = Set.of(
        "local", "mock", "fixture", "synthetic", "dummy", "fake", "debug"
    );

    private final Environment environment;
    private final DocmanOidcLoginProperties oidcProperties;
    private final DocmanAuthorizationProperties authorizationProperties;

    @PostConstruct
    public void verifyIdentityConfiguration() {
        Set<String> profiles = Set.copyOf(Arrays.asList(environment.getActiveProfiles()));
        boolean local = profiles.contains(LOCAL_PROFILE);
        boolean prod = profiles.contains(PROD_PROFILE);

        verifyOidc(local, prod);
        verifyAuthorization(local);
    }

    private void verifyOidc(boolean local, boolean prod) {
        if (!oidcProperties.enabled()) {
            return;
        }
        if (isBlank(oidcProperties.canonicalIssuer())) {
            throw new IllegalStateException(
                "OIDC 登录已启用但未显式配置 canonical issuer，拒绝启动");
        }
        if (!oidcProperties.isLoginConfigured()) {
            throw new IllegalStateException(
                "OIDC 登录已启用但配置不完整，拒绝在不完整配置下启动");
        }
        if (!local) {
            rejectSyntheticCanonicalIssuer();
            rejectCanonicalIssuerEqualToRawIssuer();
        }
        rejectSyntheticEndpoint(oidcProperties.issuer(), "issuer");
        rejectSyntheticEndpoint(oidcProperties.jwksUri(), "jwks-uri");
        rejectSyntheticEndpoint(oidcProperties.authorizationUri(), "authorization-uri");
        rejectSyntheticEndpoint(oidcProperties.tokenUri(), "token-uri");
        if (prod) {
            rejectLoopbackEndpoint(oidcProperties.issuer(), "issuer");
            rejectLoopbackEndpoint(oidcProperties.jwksUri(), "jwks-uri");
            rejectLoopbackEndpoint(oidcProperties.authorizationUri(), "authorization-uri");
            rejectLoopbackEndpoint(oidcProperties.tokenUri(), "token-uri");
        }
    }

    private void verifyAuthorization(boolean local) {
        if (!local) {
            rejectSyntheticAuthorizationConfig();
        }
        if (!authorizationProperties.isEnabled()) {
            return;
        }
        if (!authorizationProperties.isConfigured()) {
            throw new IllegalStateException(
                "授权中心客户端已启用但配置不完整，拒绝在不完整配置下启动");
        }
        ClientConfig config = authorizationProperties.toClientConfig();
        if (local) {
            return;
        }
        if (config.provider() == ClientConfig.Provider.FIXTURE) {
            throw new IllegalStateException(
                "非 local 环境禁止使用 FIXTURE 授权提供方");
        }
        if (config.mode() == ClientConfig.Mode.LOCAL) {
            throw new IllegalStateException(
                "非 local 环境禁止使用 LOCAL 授权模式");
        }
    }

    private void rejectSyntheticAuthorizationConfig() {
        rejectSyntheticAuthorizationValue(authorizationProperties.getMode(), "mode");
        rejectSyntheticAuthorizationValue(authorizationProperties.getProvider(), "provider");
        rejectSyntheticAuthorizationValue(authorizationProperties.getEnvironment(), "environment");
        rejectSyntheticAuthorizationValue(authorizationProperties.getApplication(), "application");
        rejectSyntheticAuthorizationValue(authorizationProperties.getBaseUrl(), "base-url");
        String scenario = authorizationProperties.getFixtureScenario();
        if (scenario != null && !scenario.isBlank()) {
            throw new IllegalStateException(
                "非 local 环境禁止配置授权 fixture 场景，拒绝启动");
        }
    }

    private static void rejectSyntheticAuthorizationValue(String value, String field) {
        if (value == null || value.isBlank()) {
            return;
        }
        String normalized = value.toLowerCase(Locale.ROOT);
        for (String token : SYNTHETIC_AUTHORIZATION_TOKENS) {
            if (normalized.contains(token)) {
                throw new IllegalStateException(
                    "授权配置 " + field + " 含合成/调试含义，非 local 环境拒绝启动");
            }
        }
    }

    private void rejectSyntheticEndpoint(String value, String field) {
        URI uri = parse(value, field);
        String haystack = (uri.getHost() == null ? "" : uri.getHost())
            + " " + uri.getPath();
        String normalized = haystack.toLowerCase(Locale.ROOT);
        for (String token : SYNTHETIC_TOKENS) {
            if (normalized.contains(token)) {
                throw new IllegalStateException(
                    "身份端点 " + field + " 指向合成/调试地址，拒绝启动");
            }
        }
    }

    private void rejectSyntheticCanonicalIssuer() {
        String value = oidcProperties.canonicalIssuer();
        if (isBlank(value)) {
            return;
        }
        String normalized = value.toLowerCase(Locale.ROOT);
        for (String token : SYNTHETIC_TOKENS) {
            if (normalized.contains(token)) {
                throw new IllegalStateException(
                    "canonical issuer 含合成/调试含义，非 local 环境拒绝启动");
            }
        }
    }

    private void rejectCanonicalIssuerEqualToRawIssuer() {
        String canonical = oidcProperties.canonicalIssuer();
        String issuer = oidcProperties.issuer();
        if (isBlank(canonical) || isBlank(issuer)) {
            return;
        }
        if (canonical.trim().equalsIgnoreCase(issuer.trim())) {
            throw new IllegalStateException(
                "canonical issuer 不得直接复用原始 issuer URL，拒绝启动");
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private void rejectLoopbackEndpoint(String value, String field) {
        URI uri = parse(value, field);
        String host = uri.getHost();
        if (host == null) {
            return;
        }
        String normalized = host.toLowerCase(Locale.ROOT);
        if (normalized.equals("localhost") || normalized.equals("127.0.0.1")
            || normalized.equals("::1") || normalized.equals("0.0.0.0")) {
            throw new IllegalStateException(
                "生产环境身份端点 " + field + " 不允许指向本机回环地址");
        }
    }

    private static URI parse(String value, String field) {
        try {
            URI uri = URI.create(value.trim());
            String scheme = uri.getScheme();
            if (scheme == null
                || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))) {
                throw new IllegalStateException("身份端点 " + field + " 不是合法的 http(s) 地址");
            }
            return uri;
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException("身份端点 " + field + " 不是合法的 URI", ex);
        }
    }
}
