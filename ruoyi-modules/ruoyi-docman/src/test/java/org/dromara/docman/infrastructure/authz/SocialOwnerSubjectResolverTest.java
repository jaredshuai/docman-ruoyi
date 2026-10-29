package org.dromara.docman.infrastructure.authz;

import org.dromara.docman.application.authz.CanonicalPrincipal;
import org.dromara.system.domain.vo.SysSocialVo;
import org.dromara.system.service.ISysSocialService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("dev")
@Tag("prod")
@Tag("local")
class SocialOwnerSubjectResolverTest {

    private static final Long OWNER_ID = 9L;
    private static final String TENANT_ID = "000000";
    private static final String ISSUER = "casdoor-jiaxing-company";
    private static final String SUBJECT = "casdoor-subject-009";

    @Mock
    private ISysSocialService sysSocialService;

    private SocialOwnerSubjectResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new SocialOwnerSubjectResolver(sysSocialService);
    }

    @Test
    void resolvesCanonicalOidcBinding() {
        when(sysSocialService.queryListByUserId(OWNER_ID))
            .thenReturn(List.of(binding(TENANT_ID, authId(ISSUER, SUBJECT))));

        Optional<CanonicalPrincipal> resolved = resolver.resolve(OWNER_ID, TENANT_ID);

        assertEquals(Optional.of(new CanonicalPrincipal(ISSUER, SUBJECT)), resolved);
    }

    @Test
    void resolvesUnicodeIssuerAndSubject() {
        String issuer = "casdoor-租户-canonical";
        String subject = "张三-subject-009";
        when(sysSocialService.queryListByUserId(OWNER_ID))
            .thenReturn(List.of(binding(TENANT_ID, authId(issuer, subject))));

        Optional<CanonicalPrincipal> resolved = resolver.resolve(OWNER_ID, TENANT_ID);

        assertEquals(Optional.of(new CanonicalPrincipal(issuer, subject)), resolved);
    }

    @Test
    void missingBindingFailsClosed() {
        when(sysSocialService.queryListByUserId(OWNER_ID)).thenReturn(List.of());

        assertTrue(resolver.resolve(OWNER_ID, TENANT_ID).isEmpty());
    }

    @Test
    void nullBindingListFailsClosed() {
        when(sysSocialService.queryListByUserId(OWNER_ID)).thenReturn(null);

        assertTrue(resolver.resolve(OWNER_ID, TENANT_ID).isEmpty());
    }

    @Test
    void multipleBindingsFailClosed() {
        when(sysSocialService.queryListByUserId(OWNER_ID)).thenReturn(List.of(
            binding(TENANT_ID, authId(ISSUER, SUBJECT)),
            binding(TENANT_ID, authId(ISSUER, "casdoor-subject-010"))
        ));

        assertTrue(resolver.resolve(OWNER_ID, TENANT_ID).isEmpty());
    }

    @Test
    void duplicateBindingsStillFailClosedOnNonUniqueCount() {
        String authId = authId(ISSUER, SUBJECT);
        when(sysSocialService.queryListByUserId(OWNER_ID)).thenReturn(List.of(
            binding(TENANT_ID, authId),
            binding(TENANT_ID, authId)
        ));

        assertTrue(resolver.resolve(OWNER_ID, TENANT_ID).isEmpty());
    }

    @Test
    void tenantMismatchFailsClosed() {
        when(sysSocialService.queryListByUserId(OWNER_ID))
            .thenReturn(List.of(binding("999999", authId(ISSUER, SUBJECT))));

        assertTrue(resolver.resolve(OWNER_ID, TENANT_ID).isEmpty());
    }

    @Test
    void nonOidcAuthIdFailsClosed() {
        when(sysSocialService.queryListByUserId(OWNER_ID))
            .thenReturn(List.of(binding(TENANT_ID, "github:123456")));

        assertTrue(resolver.resolve(OWNER_ID, TENANT_ID).isEmpty());
    }

    @Test
    void usernameOrRawUserIdIsNeverTreatedAsSubject() {
        when(sysSocialService.queryListByUserId(OWNER_ID))
            .thenReturn(List.of(binding(TENANT_ID, "alice")));
        assertTrue(resolver.resolve(OWNER_ID, TENANT_ID).isEmpty());

        when(sysSocialService.queryListByUserId(OWNER_ID))
            .thenReturn(List.of(binding(TENANT_ID, String.valueOf(OWNER_ID))));
        assertTrue(resolver.resolve(OWNER_ID, TENANT_ID).isEmpty());
    }

    @Test
    void invalidBase64FailsClosed() {
        when(sysSocialService.queryListByUserId(OWNER_ID))
            .thenReturn(List.of(binding(TENANT_ID, "oidc:iss=%%%:sub=" + encode(SUBJECT))));

        assertTrue(resolver.resolve(OWNER_ID, TENANT_ID).isEmpty());
    }

    @Test
    void invalidSubjectBase64FailsClosed() {
        when(sysSocialService.queryListByUserId(OWNER_ID))
            .thenReturn(List.of(binding(TENANT_ID, "oidc:iss=" + encode(ISSUER) + ":sub=***")));

        assertTrue(resolver.resolve(OWNER_ID, TENANT_ID).isEmpty());
    }

    @Test
    void malformedUtf8FailsClosed() {
        String encodedIssuer = Base64.getUrlEncoder().withoutPadding()
            .encodeToString(new byte[]{(byte) 0xC3, (byte) 0x28});
        when(sysSocialService.queryListByUserId(OWNER_ID))
            .thenReturn(List.of(binding(TENANT_ID, "oidc:iss=" + encodedIssuer + ":sub=" + encode(SUBJECT))));

        assertTrue(resolver.resolve(OWNER_ID, TENANT_ID).isEmpty());
    }

    @Test
    void missingSubMarkerFailsClosed() {
        when(sysSocialService.queryListByUserId(OWNER_ID))
            .thenReturn(List.of(binding(TENANT_ID, "oidc:iss=" + encode(ISSUER))));

        assertTrue(resolver.resolve(OWNER_ID, TENANT_ID).isEmpty());
    }

    @Test
    void repeatedSubMarkerFailsClosed() {
        when(sysSocialService.queryListByUserId(OWNER_ID))
            .thenReturn(List.of(binding(TENANT_ID,
                "oidc:iss=" + encode(ISSUER) + ":sub=" + encode(SUBJECT) + ":sub=" + encode("other"))));

        assertTrue(resolver.resolve(OWNER_ID, TENANT_ID).isEmpty());
    }

    @Test
    void emptyIssuerSegmentFailsClosed() {
        when(sysSocialService.queryListByUserId(OWNER_ID))
            .thenReturn(List.of(binding(TENANT_ID, "oidc:iss=:sub=" + encode(SUBJECT))));

        assertTrue(resolver.resolve(OWNER_ID, TENANT_ID).isEmpty());
    }

    @Test
    void blankAuthIdFailsClosed() {
        when(sysSocialService.queryListByUserId(OWNER_ID))
            .thenReturn(List.of(binding(TENANT_ID, "  ")));

        assertTrue(resolver.resolve(OWNER_ID, TENANT_ID).isEmpty());
    }

    @Test
    void missingOwnerIdOrTenantFailsClosed() {
        assertTrue(resolver.resolve(null, TENANT_ID).isEmpty());
        assertTrue(resolver.resolve(OWNER_ID, null).isEmpty());
        assertTrue(resolver.resolve(OWNER_ID, " ").isEmpty());
    }

    @Test
    void repositoryFailureFailsClosed() {
        when(sysSocialService.queryListByUserId(OWNER_ID))
            .thenThrow(new IllegalStateException("database unavailable"));

        assertTrue(resolver.resolve(OWNER_ID, TENANT_ID).isEmpty());
    }

    @Test
    void bindingWithNullAuthIdFailsClosed() {
        when(sysSocialService.queryListByUserId(OWNER_ID))
            .thenReturn(List.of(binding(TENANT_ID, null)));

        assertTrue(resolver.resolve(OWNER_ID, TENANT_ID).isEmpty());
    }

    private static SysSocialVo binding(String tenantId, String authId) {
        SysSocialVo binding = new SysSocialVo();
        binding.setUserId(OWNER_ID);
        binding.setTenantId(tenantId);
        binding.setAuthId(authId);
        return binding;
    }

    private static String authId(String issuer, String subject) {
        return "oidc:iss=" + encode(issuer) + ":sub=" + encode(subject);
    }

    private static String encode(String value) {
        return Base64.getUrlEncoder().withoutPadding()
            .encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }
}
