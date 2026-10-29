package org.dromara.web.service.identity;

import org.dromara.common.core.constant.SystemConstants;
import org.dromara.system.domain.vo.SysSocialVo;
import org.dromara.system.domain.vo.SysUserVo;
import org.dromara.system.mapper.SysUserMapper;
import org.dromara.system.service.ISysSocialService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@Tag("dev")
@Tag("prod")
@Tag("local")
@ExtendWith(MockitoExtension.class)
class CanonicalIdentityBindingServiceTest {

    private static final String ISSUER = "casdoor-jiaxing-company";
    private static final String SUBJECT = "subject-123";
    private static final String TENANT = "000000";

    @Mock
    private ISysSocialService sysSocialService;

    @Mock
    private SysUserMapper sysUserMapper;

    private CanonicalIdentityBindingService service;

    @BeforeEach
    void setUp() {
        service = new CanonicalIdentityBindingService(sysSocialService, sysUserMapper);
    }

    @Test
    void blankInputsReturnEmptyWithoutLookup() {
        assertAll(
            () -> assertTrue(service.resolve(null, SUBJECT, TENANT).isEmpty()),
            () -> assertTrue(service.resolve("", SUBJECT, TENANT).isEmpty()),
            () -> assertTrue(service.resolve("   ", SUBJECT, TENANT).isEmpty()),
            () -> assertTrue(service.resolve(ISSUER, null, TENANT).isEmpty()),
            () -> assertTrue(service.resolve(ISSUER, "  ", TENANT).isEmpty()),
            () -> assertTrue(service.resolve(ISSUER, SUBJECT, null).isEmpty()),
            () -> assertTrue(service.resolve(ISSUER, SUBJECT, "\t").isEmpty())
        );
        verifyNoInteractions(sysSocialService, sysUserMapper);
    }

    @Test
    void authIdIsNamespacedAndCoversIssuerAndSubject() {
        String authId = identity().authId();
        String otherIssuer = CanonicalIdentity.of("other-canonical-issuer", SUBJECT, TENANT)
            .orElseThrow().authId();
        String otherSubject = CanonicalIdentity.of(ISSUER, "subject-456", TENANT)
            .orElseThrow().authId();
        String otherTenant = CanonicalIdentity.of(ISSUER, SUBJECT, "other-tenant")
            .orElseThrow().authId();

        assertAll(
            () -> assertTrue(authId.startsWith("oidc:iss=")),
            () -> assertTrue(authId.contains(":sub=")),
            () -> assertFalse(authId.contains(ISSUER)),
            () -> assertFalse(authId.contains(SUBJECT)),
            () -> assertNotEquals(authId, otherIssuer),
            () -> assertNotEquals(authId, otherSubject),
            () -> assertEquals(authId, otherTenant)
        );
    }

    @Test
    void resolvesUniqueBindingInRequestedTenant() {
        when(sysSocialService.selectByAuthId(identity().authId()))
            .thenReturn(List.of(binding(42L, TENANT)));
        when(sysUserMapper.selectVoById(42L))
            .thenReturn(user(42L, TENANT, SystemConstants.NORMAL));

        Optional<CanonicalIdentityBinding> result = service.resolve(ISSUER, SUBJECT, TENANT);

        assertAll(
            () -> assertTrue(result.isPresent()),
            () -> assertEquals(42L, result.orElseThrow().userId()),
            () -> assertEquals(TENANT, result.orElseThrow().tenantId())
        );
        verify(sysSocialService).selectByAuthId(identity().authId());
        verifyNoMoreInteractions(sysSocialService);
        verify(sysUserMapper).selectVoById(42L);
        verifyNoMoreInteractions(sysUserMapper);
    }

    @Test
    void bindingInOtherTenantFailsClosed() {
        when(sysSocialService.selectByAuthId(identity().authId()))
            .thenReturn(List.of(binding(42L, "other-tenant")));

        assertTrue(service.resolve(ISSUER, SUBJECT, TENANT).isEmpty());
        verifyNoInteractions(sysUserMapper);
    }

    @Test
    void disabledUserFailsClosed() {
        when(sysSocialService.selectByAuthId(identity().authId()))
            .thenReturn(List.of(binding(42L, TENANT)));
        when(sysUserMapper.selectVoById(42L))
            .thenReturn(user(42L, TENANT, SystemConstants.DISABLE));

        assertTrue(service.resolve(ISSUER, SUBJECT, TENANT).isEmpty());
    }

    @Test
    void multipleBindingsInSameTenantFailClosed() {
        when(sysSocialService.selectByAuthId(identity().authId()))
            .thenReturn(List.of(binding(42L, TENANT), binding(43L, TENANT)));

        assertTrue(service.resolve(ISSUER, SUBJECT, TENANT).isEmpty());
        verify(sysUserMapper, never()).selectVoById(anyLong());
    }

    @Test
    void multipleBindingsAcrossTenantsResolveTheOnlyMatch() {
        when(sysSocialService.selectByAuthId(identity().authId()))
            .thenReturn(List.of(binding(42L, "other-tenant"), binding(43L, TENANT)));
        when(sysUserMapper.selectVoById(43L))
            .thenReturn(user(43L, TENANT, SystemConstants.NORMAL));

        Optional<CanonicalIdentityBinding> result = service.resolve(ISSUER, SUBJECT, TENANT);

        assertEquals(43L, result.orElseThrow().userId());
    }

    @Test
    void unknownBindingFailsClosed() {
        when(sysSocialService.selectByAuthId(identity().authId())).thenReturn(List.of());

        assertTrue(service.resolve(ISSUER, SUBJECT, TENANT).isEmpty());
        verifyNoInteractions(sysUserMapper);
    }

    @Test
    void missingUserFailsClosed() {
        when(sysSocialService.selectByAuthId(identity().authId()))
            .thenReturn(List.of(binding(42L, TENANT)));
        when(sysUserMapper.selectVoById(42L)).thenReturn(null);

        assertTrue(service.resolve(ISSUER, SUBJECT, TENANT).isEmpty());
    }

    @Test
    void userFromDifferentTenantFailsClosed() {
        when(sysSocialService.selectByAuthId(identity().authId()))
            .thenReturn(List.of(binding(42L, TENANT)));
        when(sysUserMapper.selectVoById(42L))
            .thenReturn(user(42L, "other-tenant", SystemConstants.NORMAL));

        assertTrue(service.resolve(ISSUER, SUBJECT, TENANT).isEmpty());
    }

    private static CanonicalIdentity identity() {
        return CanonicalIdentity.of(ISSUER, SUBJECT, TENANT).orElseThrow();
    }

    private static SysSocialVo binding(Long userId, String tenantId) {
        SysSocialVo vo = new SysSocialVo();
        vo.setUserId(userId);
        vo.setTenantId(tenantId);
        vo.setAuthId(identity().authId());
        return vo;
    }

    private static SysUserVo user(Long userId, String tenantId, String status) {
        SysUserVo vo = new SysUserVo();
        vo.setUserId(userId);
        vo.setTenantId(tenantId);
        vo.setStatus(status);
        return vo;
    }
}
