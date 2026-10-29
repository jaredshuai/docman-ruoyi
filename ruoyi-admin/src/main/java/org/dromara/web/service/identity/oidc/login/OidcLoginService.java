package org.dromara.web.service.identity.oidc.login;

import org.dromara.common.core.constant.SystemConstants;
import org.dromara.common.core.domain.model.LoginUser;
import org.dromara.system.domain.vo.SysUserVo;
import org.dromara.system.mapper.SysUserMapper;
import org.dromara.web.service.SysLoginService;
import org.dromara.web.service.identity.CanonicalIdentity;
import org.dromara.web.service.identity.CanonicalIdentityBinding;
import org.dromara.web.service.identity.CanonicalIdentityBindingService;
import org.dromara.web.service.identity.oidc.CasdoorOidcTokenVerifier;
import org.dromara.web.service.identity.oidc.VerifiedOidcToken;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Objects;

/**
 * Casdoor authorization-code + PKCE 登录编排。
 * <p>
 * 只信任 {@link CasdoorOidcTokenVerifier} 校验通过的原始 issuer URL 与 sub；验证成功后使用显式配置的
 * canonical issuer key 转换为 {@link CanonicalIdentity}，再通过既有的只读绑定服务找到唯一、启用且
 * 租户匹配的本地用户。任何 state/nonce/签名/issuer/audience 失败、canonical issuer 缺失、
 * 未绑定/停用/多绑定/租户不匹配、provider 或协议失败都不会退化为本地默认身份。
 */
@Service
public class OidcLoginService {

    private static final int STATE_SAVE_ATTEMPTS = 3;

    private final DocmanOidcLoginProperties properties;
    private final OidcLoginTransactionStore transactionStore;
    private final OidcTokenEndpointClient tokenEndpointClient;
    private final CasdoorOidcTokenVerifier tokenVerifier;
    private final CanonicalIdentityBindingService bindingService;
    private final SysUserMapper userMapper;
    private final SysLoginService loginService;
    private final OidcSessionAuthenticator sessionAuthenticator;

    public OidcLoginService(DocmanOidcLoginProperties properties,
                            OidcLoginTransactionStore transactionStore,
                            OidcTokenEndpointClient tokenEndpointClient,
                            CasdoorOidcTokenVerifier tokenVerifier,
                            CanonicalIdentityBindingService bindingService,
                            SysUserMapper userMapper,
                            SysLoginService loginService,
                            OidcSessionAuthenticator sessionAuthenticator) {
        this.properties = properties;
        this.transactionStore = transactionStore;
        this.tokenEndpointClient = tokenEndpointClient;
        this.tokenVerifier = tokenVerifier;
        this.bindingService = bindingService;
        this.userMapper = userMapper;
        this.loginService = loginService;
        this.sessionAuthenticator = sessionAuthenticator;
    }

    /**
     * 生成一次性 state/nonce/PKCE 参数，并返回 Casdoor authorization endpoint 跳转地址。
     */
    public String buildAuthorizationUrl() {
        requireLoginConfigured();

        String state = null;
        OidcLoginTransaction transaction = null;
        for (int attempt = 0; attempt < STATE_SAVE_ATTEMPTS; attempt++) {
            String candidateState = Pkce.newState();
            OidcLoginTransaction candidate = new OidcLoginTransaction(
                Pkce.newNonce(), Pkce.newCodeVerifier(), System.currentTimeMillis());
            if (transactionStore.save(candidateState, candidate, properties.stateTtl())) {
                state = candidateState;
                transaction = candidate;
                break;
            }
        }
        if (transaction == null) {
            throw OidcLoginException.unavailable("OIDC login state could not be created");
        }

        try {
            return UriComponentsBuilder.fromUriString(properties.authorizationUri())
                .queryParam("response_type", "code")
                .queryParam("client_id", properties.effectiveClientId())
                .queryParam("redirect_uri", properties.redirectUri())
                .queryParam("scope", properties.scope())
                .queryParam("state", state)
                .queryParam("nonce", transaction.nonce())
                .queryParam("code_challenge", Pkce.challenge(transaction.codeVerifier()))
                .queryParam("code_challenge_method", "S256")
                .build()
                .encode()
                .toUriString();
        } catch (IllegalArgumentException ex) {
            throw OidcLoginException.unavailable("OIDC authorization endpoint is invalid");
        }
    }

    /**
     * 消费一次性 state，完成 token 交换、ID Token 校验、绑定查询和本地会话登录。
     */
    public OidcLoginResult callback(String code, String state, String error) {
        requireLoginConfigured();
        if (isBlank(state)) {
            throw OidcLoginException.unauthenticated("OIDC state is required");
        }

        OidcLoginTransaction transaction = transactionStore.consume(state)
            .orElseThrow(() -> OidcLoginException.unauthenticated(
                "OIDC state is unknown, expired, or already used"));

        if (!isBlank(error)) {
            throw OidcLoginException.unavailable("OIDC provider returned an authorization error");
        }
        if (isBlank(code)) {
            throw OidcLoginException.unavailable("OIDC authorization code is missing");
        }

        String idToken;
        try {
            idToken = tokenEndpointClient.exchangeIdToken(new OidcTokenExchangeRequest(
                code,
                properties.redirectUri(),
                properties.effectiveClientId(),
                properties.clientSecret(),
                transaction.codeVerifier()
            ));
        } catch (OidcLoginException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            throw OidcLoginException.unavailable("OIDC token exchange failed");
        }

        VerifiedOidcToken verified = tokenVerifier.verify(idToken, transaction.nonce())
            .orElseThrow(() -> OidcLoginException.unauthenticated("OIDC ID token verification failed"));

        CanonicalIdentity identity = CanonicalIdentity.of(
                properties.canonicalIssuer(), verified.subject(), verified.tenantId())
            .orElseThrow(() -> OidcLoginException.unavailable("OIDC canonical issuer is not configured"));

        CanonicalIdentityBinding binding = bindingService
            .resolve(identity.issuer(), identity.subject(), identity.tenantId())
            .orElseThrow(() -> OidcLoginException.forbidden(
                "OIDC identity is not bound to exactly one enabled local user"));

        SysUserVo user = userMapper.selectVoById(binding.userId());
        if (user == null
            || SystemConstants.DISABLE.equals(user.getStatus())
            || !Objects.equals(binding.tenantId(), user.getTenantId())) {
            throw OidcLoginException.forbidden(
                "OIDC identity is not bound to an enabled local user in the configured tenant");
        }

        LoginUser loginUser = loginService.buildLoginUser(user);
        loginUser.setCanonicalIssuer(identity.issuer());
        loginUser.setCanonicalSubject(identity.subject());
        loginUser.setClientKey(properties.effectiveClientId());

        String accessToken = sessionAuthenticator.login(loginUser, properties.effectiveClientId());
        if (isBlank(accessToken)) {
            throw OidcLoginException.unavailable("local session could not be established");
        }
        return new OidcLoginResult(accessToken, properties.effectiveClientId(), properties.scope());
    }

    private void requireLoginConfigured() {
        if (properties == null
            || !properties.isLoginConfigured()
            || isBlank(properties.effectiveClientId())) {
            throw OidcLoginException.unavailable("OIDC login is not configured");
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
