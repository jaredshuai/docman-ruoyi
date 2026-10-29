package org.dromara.web.service.identity;

/**
 * 规范化外部身份对应的本地用户标识。
 *
 * @param userId   本地用户ID
 * @param tenantId 本地租户ID
 */
public record CanonicalIdentityBinding(Long userId, String tenantId) {
}
