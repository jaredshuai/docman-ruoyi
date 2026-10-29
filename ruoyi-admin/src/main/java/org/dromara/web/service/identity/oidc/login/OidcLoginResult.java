package org.dromara.web.service.identity.oidc.login;

/**
 * 登录成功后的本地会话结果。
 *
 * @param accessToken 本地 Sa-Token access token
 * @param clientId    本次登录使用的 client_id
 * @param scope       请求的授权 scope
 */
public record OidcLoginResult(String accessToken, String clientId, String scope) {
}
