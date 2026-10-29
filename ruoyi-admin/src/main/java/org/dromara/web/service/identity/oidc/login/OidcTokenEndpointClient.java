package org.dromara.web.service.identity.oidc.login;

/**
 * 向授权提供方 token endpoint 交换 ID Token 的最小客户端合同。
 */
public interface OidcTokenEndpointClient {

    /**
     * 使用 authorization code + PKCE 交换 ID Token 原始串。
     *
     * @throws OidcLoginException provider 不可用、超时或协议异常时抛 503
     */
    String exchangeIdToken(OidcTokenExchangeRequest request);
}
