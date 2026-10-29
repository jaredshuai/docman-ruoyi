package org.dromara.web.service.identity.oidc.login;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * PKCE S256 与一次性随机参数的生成器。
 * <p>
 * 所有随机值都由 {@link SecureRandom} 生成，并以无填充 base64url 编码。
 * state、nonce 与 code verifier 只保存在服务端事务存储中，不接受外部输入覆盖。
 */
public final class Pkce {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int TOKEN_BYTES = 32;

    private Pkce() {
    }

    /**
     * 生成 RFC 7636 code verifier（32 字节随机数，base64url 无填充，长度为 43）。
     */
    public static String newCodeVerifier() {
        return randomToken();
    }

    /**
     * 生成一次性 state。
     */
    public static String newState() {
        return randomToken();
    }

    /**
     * 生成一次性 nonce。
     */
    public static String newNonce() {
        return randomToken();
    }

    /**
     * 计算 S256 code challenge：base64url(SHA-256(verifier))，无填充。
     */
    public static String challenge(String codeVerifier) {
        if (codeVerifier == null || codeVerifier.isBlank()) {
            throw new IllegalArgumentException("code verifier must not be blank");
        }
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                .digest(codeVerifier.getBytes(StandardCharsets.US_ASCII));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is required for PKCE S256", ex);
        }
    }

    private static String randomToken() {
        byte[] value = new byte[TOKEN_BYTES];
        RANDOM.nextBytes(value);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
    }
}
