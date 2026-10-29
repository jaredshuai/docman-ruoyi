package org.dromara.web.service.identity;

import org.dromara.docman.config.DocmanAuthorizationProperties;
import org.dromara.web.service.identity.oidc.login.DocmanOidcLoginProperties;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("dev")
@Tag("prod")
@Tag("local")
class IdentitySafetyGuardTest {

    private static final String ISSUER = "https://casdoor.example.test";
    private static final String JWKS_URI = "https://casdoor.example.test/.well-known/jwks";
    private static final String AUTHORIZATION_URI = "https://casdoor.example.test/login/oauth/authorize";
    private static final String TOKEN_URI = "https://casdoor.example.test/api/login/oauth/access_token";
    private static final String REDIRECT_URI = "https://docman.example.test/auth/oidc/callback";
    private static final String CANONICAL_ISSUER = "casdoor-jiaxing-company";

    @Mock
    private Environment environment;

    @Test
    void disabledIdentityFeaturesPass() {
        assertDoesNotThrow(() -> guard("dev").verifyIdentityConfiguration());
    }

    @Test
    void localProfileAllowsExplicitFixtureProvider() {
        DocmanAuthorizationProperties authorization = fixtureAuthorization();

        assertDoesNotThrow(() -> new IdentitySafetyGuard(environmentFor("local"), disabledOidc(), authorization)
            .verifyIdentityConfiguration());
    }

    @Test
    void devProfileRejectsFixtureProvider() {
        IdentitySafetyGuard guard = new IdentitySafetyGuard(environmentFor("dev"), disabledOidc(), fixtureAuthorization());

        assertThrows(IllegalStateException.class, guard::verifyIdentityConfiguration);
    }

    @Test
    void devProfileRejectsLocalAuthorizationMode() {
        DocmanAuthorizationProperties authorization = httpAuthorization();
        authorization.setMode("LOCAL");

        IdentitySafetyGuard guard = new IdentitySafetyGuard(environmentFor("dev"), disabledOidc(), authorization);

        assertThrows(IllegalStateException.class, guard::verifyIdentityConfiguration);
    }

    @Test
    void devProfileAcceptsExplicitHttpAuthorization() {
        IdentitySafetyGuard guard = new IdentitySafetyGuard(environmentFor("dev"), disabledOidc(), httpAuthorization());

        assertDoesNotThrow(guard::verifyIdentityConfiguration);
    }

    @Test
    void enabledAuthorizationWithoutProviderFailsClosed() {
        DocmanAuthorizationProperties authorization = new DocmanAuthorizationProperties();
        authorization.setEnabled(true);

        IdentitySafetyGuard guard = new IdentitySafetyGuard(environmentFor("dev"), disabledOidc(), authorization);

        assertThrows(IllegalStateException.class, guard::verifyIdentityConfiguration);
    }

    @Test
    void enabledOidcWithoutCompleteConfigurationFailsClosed() {
        DocmanAuthorizationProperties authorization = new DocmanAuthorizationProperties();
        DocmanOidcLoginProperties oidc = new DocmanOidcLoginProperties(
            true, ISSUER, CANONICAL_ISSUER, JWKS_URI, "client", "client", "", AUTHORIZATION_URI, TOKEN_URI,
            REDIRECT_URI, "000000", "openid", null, null, null);

        IdentitySafetyGuard guard = new IdentitySafetyGuard(environmentFor("dev"), oidc, authorization);

        assertThrows(IllegalStateException.class, guard::verifyIdentityConfiguration);
    }

    @Test
    void enabledOidcWithoutCanonicalIssuerFailsClosedInEveryProfile() {
        DocmanOidcLoginProperties missing = oidc(ISSUER, null, JWKS_URI, AUTHORIZATION_URI, TOKEN_URI);

        assertAll(
            () -> assertThrows(IllegalStateException.class, () -> new IdentitySafetyGuard(
                environmentFor("local"), missing, new DocmanAuthorizationProperties()).verifyIdentityConfiguration()),
            () -> assertThrows(IllegalStateException.class, () -> new IdentitySafetyGuard(
                environmentFor("dev"), missing, new DocmanAuthorizationProperties()).verifyIdentityConfiguration()),
            () -> assertThrows(IllegalStateException.class, () -> new IdentitySafetyGuard(
                environmentFor("release"), missing, new DocmanAuthorizationProperties()).verifyIdentityConfiguration())
        );
    }

    @Test
    void nonLocalRejectsSyntheticCanonicalIssuer() {
        DocmanOidcLoginProperties oidc = oidc(ISSUER, "mock-canonical-issuer", JWKS_URI,
            AUTHORIZATION_URI, TOKEN_URI);

        IdentitySafetyGuard guard = new IdentitySafetyGuard(
            environmentFor("dev"), oidc, new DocmanAuthorizationProperties());

        assertThrows(IllegalStateException.class, guard::verifyIdentityConfiguration);
    }

    @Test
    void nonLocalRejectsCanonicalIssuerEqualToRawIssuerUrl() {
        DocmanOidcLoginProperties oidc = oidc(ISSUER, ISSUER, JWKS_URI, AUTHORIZATION_URI, TOKEN_URI);

        IdentitySafetyGuard guard = new IdentitySafetyGuard(
            environmentFor("release"), oidc, new DocmanAuthorizationProperties());

        assertThrows(IllegalStateException.class, guard::verifyIdentityConfiguration);
    }

    @Test
    void localAcceptsExplicitCanonicalIssuer() {
        IdentitySafetyGuard guard = new IdentitySafetyGuard(
            environmentFor("local"), oidc(ISSUER, JWKS_URI, AUTHORIZATION_URI, TOKEN_URI),
            new DocmanAuthorizationProperties());

        assertDoesNotThrow(guard::verifyIdentityConfiguration);
    }

    @Test
    void prodRejectsLoopbackIssuer() {
        DocmanOidcLoginProperties oidc = oidc("http://127.0.0.1:8000", "http://127.0.0.1:8000/jwks",
            "http://127.0.0.1:8000/login/oauth/authorize", "http://127.0.0.1:8000/api/login/oauth/access_token");

        IdentitySafetyGuard guard = new IdentitySafetyGuard(
            environmentFor("prod"), oidc, new DocmanAuthorizationProperties());

        assertThrows(IllegalStateException.class, guard::verifyIdentityConfiguration);
    }

    @Test
    void nonLocalRejectsSyntheticIssuerHost() {
        DocmanOidcLoginProperties oidc = oidc("https://mock-identity.test", JWKS_URI,
            AUTHORIZATION_URI, TOKEN_URI);

        IdentitySafetyGuard guard = new IdentitySafetyGuard(
            environmentFor("dev"), oidc, new DocmanAuthorizationProperties());

        assertThrows(IllegalStateException.class, guard::verifyIdentityConfiguration);
    }

    @Test
    void prodAcceptsRealCasdoorEndpoints() {
        IdentitySafetyGuard guard = new IdentitySafetyGuard(
            environmentFor("prod"), oidc(ISSUER, JWKS_URI, AUTHORIZATION_URI, TOKEN_URI),
            httpAuthorization());

        assertDoesNotThrow(guard::verifyIdentityConfiguration);
    }

    @Test
    void disabledAuthorizationWithoutSyntheticFieldsPassesNonLocal() {
        IdentitySafetyGuard guard = new IdentitySafetyGuard(
            environmentFor("prod"), disabledOidc(), new DocmanAuthorizationProperties());

        assertDoesNotThrow(guard::verifyIdentityConfiguration);
    }

    @Test
    void disabledAuthorizationRejectsLocalMode() {
        DocmanAuthorizationProperties authorization = new DocmanAuthorizationProperties();
        authorization.setMode("LOCAL");

        IdentitySafetyGuard guard = new IdentitySafetyGuard(environmentFor("dev"), disabledOidc(), authorization);

        assertThrows(IllegalStateException.class, guard::verifyIdentityConfiguration);
    }

    @Test
    void disabledAuthorizationRejectsFixtureProvider() {
        DocmanAuthorizationProperties authorization = new DocmanAuthorizationProperties();
        authorization.setProvider("FIXTURE");

        IdentitySafetyGuard guard = new IdentitySafetyGuard(environmentFor("release"), disabledOidc(), authorization);

        assertThrows(IllegalStateException.class, guard::verifyIdentityConfiguration);
    }

    @Test
    void disabledAuthorizationRejectsFixtureScenario() {
        DocmanAuthorizationProperties authorization = new DocmanAuthorizationProperties();
        authorization.setFixtureScenario("default");

        IdentitySafetyGuard guard = new IdentitySafetyGuard(environmentFor("dev"), disabledOidc(), authorization);

        assertThrows(IllegalStateException.class, guard::verifyIdentityConfiguration);
    }

    @Test
    void disabledAuthorizationRejectsSyntheticEnvironmentValue() {
        DocmanAuthorizationProperties authorization = new DocmanAuthorizationProperties();
        authorization.setEnvironment("mock-env");

        IdentitySafetyGuard guard = new IdentitySafetyGuard(environmentFor("prod"), disabledOidc(), authorization);

        assertThrows(IllegalStateException.class, guard::verifyIdentityConfiguration);
    }

    private IdentitySafetyGuard guard(String profile) {
        return new IdentitySafetyGuard(environmentFor(profile), disabledOidc(), new DocmanAuthorizationProperties());
    }

    private Environment environmentFor(String profile) {
        when(environment.getActiveProfiles()).thenReturn(new String[]{profile});
        return environment;
    }

    private static DocmanOidcLoginProperties disabledOidc() {
        return new DocmanOidcLoginProperties(
            false, null, null, null, null, null, null, null, null, null, null, null, null, null, null);
    }

    private static DocmanOidcLoginProperties oidc(String issuer, String jwks, String authorization, String token) {
        return oidc(issuer, CANONICAL_ISSUER, jwks, authorization, token);
    }

    private static DocmanOidcLoginProperties oidc(String issuer, String canonicalIssuer, String jwks,
                                                  String authorization, String token) {
        return new DocmanOidcLoginProperties(
            true, issuer, canonicalIssuer, jwks, "docman-client", "docman-client", "runtime-secret",
            authorization, token, REDIRECT_URI, "000000", "openid", null, null, null);
    }

    private static DocmanAuthorizationProperties fixtureAuthorization() {
        DocmanAuthorizationProperties properties = new DocmanAuthorizationProperties();
        properties.setEnabled(true);
        properties.setMode("LOCAL");
        properties.setProvider("FIXTURE");
        properties.setApplication("docman");
        properties.setEnvironment("local");
        properties.setFixtureScenario("default");
        return properties;
    }

    private static DocmanAuthorizationProperties httpAuthorization() {
        DocmanAuthorizationProperties properties = new DocmanAuthorizationProperties();
        properties.setEnabled(true);
        properties.setMode("INTEGRATION");
        properties.setProvider("HTTP");
        properties.setApplication("docman");
        properties.setEnvironment("dev");
        properties.setBaseUrl("https://authz.example.test");
        properties.setServiceCredential("runtime-credential");
        return properties;
    }
}
