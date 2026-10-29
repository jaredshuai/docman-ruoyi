package org.dromara.web.service.identity.oidc.login;

/**
 * authorization-code token 交换请求。
 * <p>
 * client secret 只来自运行时配置，{@link #toString()} 不输出 secret 或 verifier。
 */
public record OidcTokenExchangeRequest(
    String code,
    String redirectUri,
    String clientId,
    String clientSecret,
    String codeVerifier
) {

    @Override
    public String toString() {
        return "OidcTokenExchangeRequest["
            + "code=<redacted>"
            + ", redirectUri=" + redirectUri
            + ", clientId=" + clientId
            + ", clientSecret=<redacted>"
            + ", codeVerifier=<redacted>"
            + ']';
    }
}
