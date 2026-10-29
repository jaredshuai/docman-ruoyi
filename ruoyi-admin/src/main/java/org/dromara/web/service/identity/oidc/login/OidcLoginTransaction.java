package org.dromara.web.service.identity.oidc.login;

/**
 * 一次登录授权请求的服务端临时事务。
 * <p>
 * 只包含 nonce 与 PKCE code verifier；state 作为存储键，不重复写入值。
 * 事务由 Redis TTL 限制生命周期，并在回调时原子消费。
 *
 * @param nonce               授权请求生成的 nonce
 * @param codeVerifier        PKCE code verifier
 * @param createdAtEpochMilli 创建时间，便于本地测试与诊断
 */
public record OidcLoginTransaction(
    String nonce,
    String codeVerifier,
    long createdAtEpochMilli
) {

    public OidcLoginTransaction {
        if (nonce == null || nonce.isBlank()) {
            throw new IllegalArgumentException("nonce must not be blank");
        }
        if (codeVerifier == null || codeVerifier.isBlank()) {
            throw new IllegalArgumentException("code verifier must not be blank");
        }
    }
}
