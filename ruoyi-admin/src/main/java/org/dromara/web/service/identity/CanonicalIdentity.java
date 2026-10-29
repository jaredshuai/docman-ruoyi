package org.dromara.web.service.identity;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;

/**
 * 规范化外部身份三元组，仅用于本地绑定关系的只读查询。
 *
 * @param issuer   规范化签发方
 * @param subject  规范化主体
 * @param tenantId 本地租户标识
 */
public record CanonicalIdentity(String issuer, String subject, String tenantId) {

    /**
     * 复用 sys_social.auth_id 时使用的命名空间前缀，避免与 JustAuth 的 source+uuid 组合冲突。
     */
    private static final String AUTH_ID_NAMESPACE = "oidc:iss=";

    private static final String SUBJECT_MARKER = ":sub=";

    /**
     * 构造规范化身份；任一输入为空白时明确拒绝。
     */
    public static Optional<CanonicalIdentity> of(String issuer, String subject, String tenantId) {
        if (isBlank(issuer) || isBlank(subject) || isBlank(tenantId)) {
            return Optional.empty();
        }
        return Optional.of(new CanonicalIdentity(issuer, subject, tenantId));
    }

    /**
     * 生成稳定且带命名空间的 authId，issuer 与 subject 同时参与组合。
     */
    public String authId() {
        return AUTH_ID_NAMESPACE + encode(issuer) + SUBJECT_MARKER + encode(subject);
    }

    private static String encode(String value) {
        return Base64.getUrlEncoder().withoutPadding()
            .encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
