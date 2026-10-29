package org.dromara.web.service.identity.oidc;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.dromara.common.core.constant.SystemConstants;
import org.dromara.common.core.domain.model.LoginUser;
import org.dromara.system.domain.vo.SysUserVo;
import org.dromara.system.mapper.SysUserMapper;
import org.dromara.web.service.SysLoginService;
import org.dromara.web.service.identity.CanonicalIdentity;
import org.dromara.web.service.identity.CanonicalIdentityBinding;
import org.dromara.web.service.identity.CanonicalIdentityBindingService;
import org.dromara.web.service.identity.oidc.login.DocmanOidcLoginProperties;
import org.dromara.web.service.identity.oidc.login.OidcLoginException;
import org.dromara.web.service.identity.oidc.login.OidcLoginResult;
import org.dromara.web.service.identity.oidc.login.OidcLoginService;
import org.dromara.web.service.identity.oidc.login.OidcLoginTransaction;
import org.dromara.web.service.identity.oidc.login.OidcLoginTransactionStore;
import org.dromara.web.service.identity.oidc.login.OidcSessionAuthenticator;
import org.dromara.web.service.identity.oidc.login.OidcTokenEndpointClient;
import org.dromara.web.service.identity.oidc.login.OidcTokenExchangeRequest;
import org.dromara.web.service.identity.oidc.login.Pkce;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("dev")
@Tag("prod")
@Tag("local")
class OidcLoginServiceTest {

    private static final String ISSUER = "https://casdoor.example.test";
    private static final String CANONICAL_ISSUER = "casdoor-jiaxing-company";
    private static final String JWKS_URI = "https://casdoor.example.test/.well-known/jwks";
    private static final String CLIENT_ID = "docman-client";
    private static final String CLIENT_SECRET = "runtime-secret-value";
    private static final String AUTHORIZATION_URI = "https://casdoor.example.test/login/oauth/authorize";
    private static final String TOKEN_URI = "https://casdoor.example.test/api/login/oauth/access_token";
    private static final String REDIRECT_URI = "https://docman.example.test/auth/oidc/callback";
    private static final String TENANT = "000000";
    private static final String SCOPE = "openid profile";
    private static final String SUBJECT = "casdoor-subject-001";
    private static final Long USER_ID = 77L;

    private static RSAPrivateKey signingKey;
    private static RSAPrivateKey foreignKey;
    private static NimbusJwtDecoder decoder;
    private static CasdoorOidcTokenVerifier verifier;

    @Mock
    private OidcLoginTransactionStore transactionStore;
    @Mock
    private OidcTokenEndpointClient tokenEndpointClient;
    @Mock
    private CanonicalIdentityBindingService bindingService;
    @Mock
    private SysUserMapper userMapper;
    @Mock
    private SysLoginService loginService;
    @Mock
    private OidcSessionAuthenticator sessionAuthenticator;

    private OidcLoginService service;

    @BeforeAll
    static void prepareKeys() throws Exception {
        KeyPair keyPair = generateKeyPair();
        signingKey = (RSAPrivateKey) keyPair.getPrivate();
        foreignKey = (RSAPrivateKey) generateKeyPair().getPrivate();
        decoder = NimbusJwtDecoder.withPublicKey((RSAPublicKey) keyPair.getPublic()).build();
        verifier = new CasdoorOidcTokenVerifier(
            new DocmanOidcProperties(true, ISSUER, JWKS_URI, CLIENT_ID, TENANT),
            decoder
        );
    }

    @BeforeEach
    void setUp() {
        service = newService(configured());
    }

    @Test
    void startFailsClosedWhenLoginIsNotConfigured() {
        OidcLoginService unconfigured = newService(new DocmanOidcLoginProperties(
            false, ISSUER, CANONICAL_ISSUER, JWKS_URI, CLIENT_ID, CLIENT_ID, CLIENT_SECRET,
            AUTHORIZATION_URI, TOKEN_URI, REDIRECT_URI, TENANT, SCOPE, null, null, null));

        OidcLoginException ex = assertThrows(OidcLoginException.class, unconfigured::buildAuthorizationUrl);

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, ex.status());
        verifyNoInteractions(transactionStore, tokenEndpointClient);
    }

    @Test
    void startFailsClosedWhenCanonicalIssuerIsMissing() {
        OidcLoginService missingCanonical = newService(new DocmanOidcLoginProperties(
            true, ISSUER, null, JWKS_URI, CLIENT_ID, CLIENT_ID, CLIENT_SECRET,
            AUTHORIZATION_URI, TOKEN_URI, REDIRECT_URI, TENANT, SCOPE, null, null, null));

        OidcLoginException ex = assertThrows(OidcLoginException.class, missingCanonical::buildAuthorizationUrl);

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, ex.status());
        verifyNoInteractions(transactionStore, tokenEndpointClient);
    }

    @Test
    void startBuildsAuthorizationUrlWithPkceAndServerSideState() {
        when(transactionStore.save(anyString(), any(OidcLoginTransaction.class), any(Duration.class)))
            .thenReturn(true);
        ArgumentCaptor<String> stateCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<OidcLoginTransaction> transactionCaptor =
            ArgumentCaptor.forClass(OidcLoginTransaction.class);

        String url = service.buildAuthorizationUrl();

        verify(transactionStore).save(stateCaptor.capture(), transactionCaptor.capture(), any(Duration.class));
        MultiValueMap<String, String> params = UriComponentsBuilder.fromUriString(url)
            .build()
            .getQueryParams();
        String state = stateCaptor.getValue();
        OidcLoginTransaction transaction = transactionCaptor.getValue();

        assertAll(
            () -> assertTrue(url.startsWith(AUTHORIZATION_URI)),
            () -> assertEquals("code", raw(params, "response_type")),
            () -> assertEquals(CLIENT_ID, raw(params, "client_id")),
            () -> assertEquals(REDIRECT_URI, raw(params, "redirect_uri")),
            () -> assertEquals(SCOPE, raw(params, "scope")),
            () -> assertEquals(state, raw(params, "state")),
            () -> assertEquals(transaction.nonce(), raw(params, "nonce")),
            () -> assertEquals(Pkce.challenge(transaction.codeVerifier()), raw(params, "code_challenge")),
            () -> assertEquals("S256", raw(params, "code_challenge_method")),
            () -> assertFalse(state.isBlank()),
            () -> assertNotEquals(state, transaction.nonce())
        );
    }

    @Test
    void startReturns503WhenStateCannotBePersisted() {
        when(transactionStore.save(anyString(), any(OidcLoginTransaction.class), any(Duration.class)))
            .thenReturn(false);

        OidcLoginException ex = assertThrows(OidcLoginException.class, service::buildAuthorizationUrl);

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, ex.status());
    }

    @Test
    void callbackRejectsMissingStateAsUnauthenticated() {
        OidcLoginException ex = assertThrows(OidcLoginException.class,
            () -> service.callback("code", "  ", null));

        assertEquals(HttpStatus.UNAUTHORIZED, ex.status());
        verifyNoInteractions(transactionStore, tokenEndpointClient);
    }

    @Test
    void callbackRejectsUnknownOrExpiredState() {
        when(transactionStore.consume("unknown-state")).thenReturn(Optional.empty());

        OidcLoginException ex = assertThrows(OidcLoginException.class,
            () -> service.callback("code", "unknown-state", null));

        assertEquals(HttpStatus.UNAUTHORIZED, ex.status());
        verifyNoInteractions(tokenEndpointClient);
    }

    @Test
    void callbackConsumesStateBeforeMappingProviderErrorsTo503() {
        when(transactionStore.consume("state-1")).thenReturn(Optional.of(transaction("nonce-1")));

        OidcLoginException ex = assertThrows(OidcLoginException.class,
            () -> service.callback(null, "state-1", "access_denied"));

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, ex.status());
        verify(transactionStore).consume("state-1");
        verifyNoInteractions(tokenEndpointClient);
    }

    @Test
    void callbackRejectsMissingCodeAs503() {
        when(transactionStore.consume("state-1")).thenReturn(Optional.of(transaction("nonce-1")));

        OidcLoginException ex = assertThrows(OidcLoginException.class,
            () -> service.callback("  ", "state-1", null));

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, ex.status());
        verifyNoInteractions(tokenEndpointClient);
    }

    @Test
    void callbackPropagatesTokenEndpointFailureAs503() {
        when(transactionStore.consume("state-1")).thenReturn(Optional.of(transaction("nonce-1")));
        when(tokenEndpointClient.exchangeIdToken(any(OidcTokenExchangeRequest.class)))
            .thenThrow(OidcLoginException.unavailable("provider down"));

        OidcLoginException ex = assertThrows(OidcLoginException.class,
            () -> service.callback("code-1", "state-1", null));

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, ex.status());
        verify(bindingService, never()).resolve(anyString(), anyString(), anyString());
    }

    @Test
    void callbackExchangesCodeWithVerifierAndClientSecret() throws Exception {
        OidcLoginTransaction transaction = transaction("nonce-1");
        when(transactionStore.consume("state-1")).thenReturn(Optional.of(transaction));
        when(tokenEndpointClient.exchangeIdToken(any(OidcTokenExchangeRequest.class)))
            .thenReturn(validToken("nonce-1"));
        when(bindingService.resolve(CANONICAL_ISSUER, SUBJECT, TENANT))
            .thenReturn(Optional.of(new CanonicalIdentityBinding(USER_ID, TENANT)));
        SysUserVo user = enabledUser();
        when(userMapper.selectVoById(USER_ID)).thenReturn(user);
        LoginUser loginUser = new LoginUser();
        when(loginService.buildLoginUser(user)).thenReturn(loginUser);
        when(sessionAuthenticator.login(any(LoginUser.class), eq(CLIENT_ID))).thenReturn("local-token");

        service.callback("code-1", "state-1", null);

        ArgumentCaptor<OidcTokenExchangeRequest> requestCaptor =
            ArgumentCaptor.forClass(OidcTokenExchangeRequest.class);
        verify(tokenEndpointClient).exchangeIdToken(requestCaptor.capture());
        OidcTokenExchangeRequest request = requestCaptor.getValue();
        assertAll(
            () -> assertEquals("code-1", request.code()),
            () -> assertEquals(REDIRECT_URI, request.redirectUri()),
            () -> assertEquals(CLIENT_ID, request.clientId()),
            () -> assertEquals(CLIENT_SECRET, request.clientSecret()),
            () -> assertEquals(transaction.codeVerifier(), request.codeVerifier())
        );
    }

    @Test
    void callbackRejectsInvalidSignature() throws Exception {
        when(transactionStore.consume("state-1")).thenReturn(Optional.of(transaction("nonce-1")));
        when(tokenEndpointClient.exchangeIdToken(any(OidcTokenExchangeRequest.class)))
            .thenReturn(token("nonce-1", foreignKey, ISSUER, CLIENT_ID, Instant.now().plusSeconds(300)));

        OidcLoginException ex = assertThrows(OidcLoginException.class,
            () -> service.callback("code-1", "state-1", null));

        assertEquals(HttpStatus.UNAUTHORIZED, ex.status());
        verifyNoInteractions(bindingService);
    }

    @Test
    void callbackRejectsNonceMismatch() throws Exception {
        when(transactionStore.consume("state-1")).thenReturn(Optional.of(transaction("nonce-1")));
        when(tokenEndpointClient.exchangeIdToken(any(OidcTokenExchangeRequest.class)))
            .thenReturn(validToken("nonce-2"));

        OidcLoginException ex = assertThrows(OidcLoginException.class,
            () -> service.callback("code-1", "state-1", null));

        assertEquals(HttpStatus.UNAUTHORIZED, ex.status());
        verifyNoInteractions(bindingService);
    }

    @Test
    void callbackRejectsExpiredToken() throws Exception {
        when(transactionStore.consume("state-1")).thenReturn(Optional.of(transaction("nonce-1")));
        when(tokenEndpointClient.exchangeIdToken(any(OidcTokenExchangeRequest.class)))
            .thenReturn(token("nonce-1", signingKey, ISSUER, CLIENT_ID, Instant.now().minusSeconds(60)));

        OidcLoginException ex = assertThrows(OidcLoginException.class,
            () -> service.callback("code-1", "state-1", null));

        assertEquals(HttpStatus.UNAUTHORIZED, ex.status());
        verifyNoInteractions(bindingService);
    }

    @Test
    void callbackRejectsWrongIssuer() throws Exception {
        when(transactionStore.consume("state-1")).thenReturn(Optional.of(transaction("nonce-1")));
        when(tokenEndpointClient.exchangeIdToken(any(OidcTokenExchangeRequest.class)))
            .thenReturn(token("nonce-1", signingKey, "https://evil.example.test", CLIENT_ID,
                Instant.now().plusSeconds(300)));

        OidcLoginException ex = assertThrows(OidcLoginException.class,
            () -> service.callback("code-1", "state-1", null));

        assertEquals(HttpStatus.UNAUTHORIZED, ex.status());
        verifyNoInteractions(bindingService);
    }

    @Test
    void callbackRejectsWrongAudience() throws Exception {
        when(transactionStore.consume("state-1")).thenReturn(Optional.of(transaction("nonce-1")));
        when(tokenEndpointClient.exchangeIdToken(any(OidcTokenExchangeRequest.class)))
            .thenReturn(token("nonce-1", signingKey, ISSUER, "another-client",
                Instant.now().plusSeconds(300)));

        OidcLoginException ex = assertThrows(OidcLoginException.class,
            () -> service.callback("code-1", "state-1", null));

        assertEquals(HttpStatus.UNAUTHORIZED, ex.status());
        verifyNoInteractions(bindingService);
    }

    @Test
    void callbackRejectsUnboundIdentityAsForbidden() throws Exception {
        when(transactionStore.consume("state-1")).thenReturn(Optional.of(transaction("nonce-1")));
        when(tokenEndpointClient.exchangeIdToken(any(OidcTokenExchangeRequest.class)))
            .thenReturn(validToken("nonce-1"));
        when(bindingService.resolve(CANONICAL_ISSUER, SUBJECT, TENANT)).thenReturn(Optional.empty());

        OidcLoginException ex = assertThrows(OidcLoginException.class,
            () -> service.callback("code-1", "state-1", null));

        assertEquals(HttpStatus.FORBIDDEN, ex.status());
        verifyNoInteractions(sessionAuthenticator);
    }

    @Test
    void callbackRejectsDisabledUserAsForbidden() throws Exception {
        SysUserVo disabled = enabledUser();
        disabled.setStatus(SystemConstants.DISABLE);
        prepareBindingTo(disabled);

        OidcLoginException ex = assertThrows(OidcLoginException.class,
            () -> service.callback("code-1", "state-1", null));

        assertEquals(HttpStatus.FORBIDDEN, ex.status());
        verifyNoInteractions(sessionAuthenticator);
    }

    @Test
    void callbackRejectsTenantMismatchAsForbidden() throws Exception {
        SysUserVo foreignTenant = enabledUser();
        foreignTenant.setTenantId("999999");
        prepareBindingTo(foreignTenant);

        OidcLoginException ex = assertThrows(OidcLoginException.class,
            () -> service.callback("code-1", "state-1", null));

        assertEquals(HttpStatus.FORBIDDEN, ex.status());
        verifyNoInteractions(sessionAuthenticator);
    }

    @Test
    void callbackEstablishesLocalSessionWithCanonicalClaims() throws Exception {
        SysUserVo user = enabledUser();
        prepareBindingTo(user);
        LoginUser loginUser = new LoginUser();
        when(loginService.buildLoginUser(user)).thenReturn(loginUser);
        when(sessionAuthenticator.login(any(LoginUser.class), eq(CLIENT_ID))).thenReturn("local-token");

        OidcLoginResult result = service.callback("code-1", "state-1", null);

        ArgumentCaptor<LoginUser> loginUserCaptor = ArgumentCaptor.forClass(LoginUser.class);
        verify(sessionAuthenticator).login(loginUserCaptor.capture(), eq(CLIENT_ID));
        LoginUser authenticated = loginUserCaptor.getValue();
        assertAll(
            () -> assertEquals("local-token", result.accessToken()),
            () -> assertEquals(CLIENT_ID, result.clientId()),
            () -> assertEquals(SCOPE, result.scope()),
            () -> assertEquals(CANONICAL_ISSUER, authenticated.getCanonicalIssuer()),
            () -> assertNotEquals(ISSUER, authenticated.getCanonicalIssuer()),
            () -> assertEquals(SUBJECT, authenticated.getCanonicalSubject()),
            () -> assertEquals(CLIENT_ID, authenticated.getClientKey()),
            () -> assertEquals(1, loginUserCaptor.getAllValues().size())
        );
        verify(bindingService).resolve(CANONICAL_ISSUER, SUBJECT, TENANT);
        verify(transactionStore).consume("state-1");
    }

    @Test
    void callbackRejectsReplayedState() throws Exception {
        SysUserVo user = enabledUser();
        when(transactionStore.consume("state-1"))
            .thenReturn(Optional.of(transaction("nonce-1")))
            .thenReturn(Optional.empty());
        when(tokenEndpointClient.exchangeIdToken(any(OidcTokenExchangeRequest.class)))
            .thenReturn(validToken("nonce-1"));
        when(bindingService.resolve(CANONICAL_ISSUER, SUBJECT, TENANT))
            .thenReturn(Optional.of(new CanonicalIdentityBinding(USER_ID, TENANT)));
        when(userMapper.selectVoById(USER_ID)).thenReturn(user);
        when(loginService.buildLoginUser(user)).thenReturn(new LoginUser());
        when(sessionAuthenticator.login(any(LoginUser.class), eq(CLIENT_ID))).thenReturn("local-token");

        service.callback("code-1", "state-1", null);
        OidcLoginException replay = assertThrows(OidcLoginException.class,
            () -> service.callback("code-1", "state-1", null));

        assertEquals(HttpStatus.UNAUTHORIZED, replay.status());
        verify(transactionStore, org.mockito.Mockito.times(2)).consume("state-1");
    }

    private void prepareBindingTo(SysUserVo user) throws Exception {
        when(transactionStore.consume("state-1")).thenReturn(Optional.of(transaction("nonce-1")));
        when(tokenEndpointClient.exchangeIdToken(any(OidcTokenExchangeRequest.class)))
            .thenReturn(validToken("nonce-1"));
        when(bindingService.resolve(CANONICAL_ISSUER, SUBJECT, TENANT))
            .thenReturn(Optional.of(new CanonicalIdentityBinding(USER_ID, TENANT)));
        when(userMapper.selectVoById(USER_ID)).thenReturn(user);
    }

    private OidcLoginService newService(DocmanOidcLoginProperties properties) {
        return new OidcLoginService(
            properties,
            transactionStore,
            tokenEndpointClient,
            verifier,
            bindingService,
            userMapper,
            loginService,
            sessionAuthenticator
        );
    }

    private static DocmanOidcLoginProperties configured() {
        return new DocmanOidcLoginProperties(
            true, ISSUER, CANONICAL_ISSUER, JWKS_URI, CLIENT_ID, CLIENT_ID, CLIENT_SECRET,
            AUTHORIZATION_URI, TOKEN_URI, REDIRECT_URI, TENANT, SCOPE, null, null, null
        );
    }

    private static OidcLoginTransaction transaction(String nonce) {
        return new OidcLoginTransaction(nonce, Pkce.newCodeVerifier(), System.currentTimeMillis());
    }

    private static SysUserVo enabledUser() {
        SysUserVo user = new SysUserVo();
        user.setUserId(USER_ID);
        user.setTenantId(TENANT);
        user.setStatus(SystemConstants.NORMAL);
        return user;
    }

    private static String validToken(String nonce) throws Exception {
        return token(nonce, signingKey, ISSUER, CLIENT_ID, Instant.now().plusSeconds(300));
    }

    private static String token(String nonce, RSAPrivateKey key, String issuer, String audience,
                                Instant expiresAt) throws Exception {
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
            .issuer(issuer)
            .subject(SUBJECT)
            .audience(List.of(audience))
            .issueTime(Date.from(Instant.now().minusSeconds(5)))
            .expirationTime(Date.from(expiresAt))
            .claim("nonce", nonce)
            .jwtID(UUID.randomUUID().toString())
            .build();
        SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.RS256), claims);
        jwt.sign(new RSASSASigner(key));
        return jwt.serialize();
    }

    private static KeyPair generateKeyPair() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        return generator.generateKeyPair();
    }

    private static String raw(MultiValueMap<String, String> params, String name) {
        List<String> values = params.get(name);
        if (values == null || values.isEmpty()) {
            return null;
        }
        return URLDecoder.decode(values.get(0), StandardCharsets.UTF_8);
    }
}
