package org.dromara.docman.infrastructure.authz;

import org.dromara.docman.application.authz.CanonicalPrincipal;
import org.dromara.docman.application.authz.OwnerSubjectResolver;
import org.dromara.system.domain.vo.SysSocialVo;
import org.dromara.system.service.ISysSocialService;
import org.springframework.stereotype.Component;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * 基于既有 {@code sys_social} 绑定的只读反向解析。
 * <p>
 * 仅接受由 canonical identity 写入的
 * {@code oidc:iss=<base64url(issuer)>:sub=<base64url(subject)>}。
 * 无绑定、多条绑定、非 OIDC 命名空间、解码失败或租户不匹配均返回 empty。
 */
@Component
public class SocialOwnerSubjectResolver implements OwnerSubjectResolver {

    static final String AUTH_ID_PREFIX = "oidc:iss=";
    static final String SUBJECT_MARKER = ":sub=";

    private final ISysSocialService sysSocialService;

    public SocialOwnerSubjectResolver(ISysSocialService sysSocialService) {
        this.sysSocialService = sysSocialService;
    }

    @Override
    public Optional<CanonicalPrincipal> resolve(Long ownerId, String tenantId) {
        if (ownerId == null || isBlank(tenantId)) {
            return Optional.empty();
        }
        List<SysSocialVo> bindings;
        try {
            bindings = sysSocialService.queryListByUserId(ownerId);
        } catch (RuntimeException ex) {
            return Optional.empty();
        }
        if (bindings == null || bindings.size() != 1) {
            return Optional.empty();
        }
        SysSocialVo binding = bindings.get(0);
        if (binding == null || !Objects.equals(tenantId, binding.getTenantId())) {
            return Optional.empty();
        }
        return parse(binding.getAuthId());
    }

    private static Optional<CanonicalPrincipal> parse(String authId) {
        if (isBlank(authId) || !authId.startsWith(AUTH_ID_PREFIX)) {
            return Optional.empty();
        }
        int marker = authId.indexOf(SUBJECT_MARKER, AUTH_ID_PREFIX.length());
        if (marker < 0 || authId.indexOf(SUBJECT_MARKER, marker + 1) >= 0) {
            return Optional.empty();
        }
        String encodedIssuer = authId.substring(AUTH_ID_PREFIX.length(), marker);
        String encodedSubject = authId.substring(marker + SUBJECT_MARKER.length());
        if (encodedIssuer.isEmpty() || encodedSubject.isEmpty()) {
            return Optional.empty();
        }
        String issuer = decode(encodedIssuer);
        String subject = decode(encodedSubject);
        if (issuer == null || subject == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(new CanonicalPrincipal(issuer, subject));
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

    private static String decode(String encoded) {
        try {
            byte[] bytes = Base64.getUrlDecoder().decode(encoded);
            if (bytes.length == 0) {
                return null;
            }
            return StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(bytes))
                .toString();
        } catch (IllegalArgumentException | CharacterCodingException ex) {
            return null;
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
