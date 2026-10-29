package org.dromara.web.service.identity.oidc;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("dev")
@Tag("prod")
@Tag("local")
class CasdoorOidcTokenVerifierTest {

    private static final String ISSUER = "https://casdoor.example.test";
    private static final String JWKS_URI = "https://casdoor.example.test/.well-known/jwks";
    private static final String AUDIENCE = "docman-client";
    private static final String TENANT = "000000";
    private static final String SUBJECT = "subject-123";
    private static final String CANONICAL_ISSUER = "casdoor-jiaxing-company";

    private static RSAPrivateKey signingKey;
    private static RSAPrivateKey foreignKey;
    private static NimbusJwtDecoder decoder;

    @BeforeAll
    static void generateInMemoryKeys() throws Exception {
        KeyPair keyPair = generateKeyPair();
        signingKey = (RSAPrivateKey) keyPair.getPrivate();
        foreignKey = (RSAPrivateKey) generateKeyPair().getPrivate();
        decoder = NimbusJwtDecoder.withPublicKey((RSAPublicKey) keyPair.getPublic()).build();
    }

    @Test
    void blankTokenFailsClosed() throws Exception {
        CasdoorOidcTokenVerifier verifier = verifier(configured());

        assertAll(
            () -> assertTrue(verifier.verify(null).isEmpty()),
            () -> assertTrue(verifier.verify("").isEmpty()),
            () -> assertTrue(verifier.verify("   ").isEmpty())
        );
    }

    @Test
    void missingConfigurationFailsClosedEvenForValidToken() throws Exception {
        String token = validToken();

        assertAll(
            () -> assertTrue(new CasdoorOidcTokenVerifier(null, decoder).verify(token).isEmpty()),
            () -> assertTrue(verifier(new DocmanOidcProperties(false, ISSUER, JWKS_URI, AUDIENCE, TENANT))
                .verify(token).isEmpty()),
            () -> assertTrue(verifier(new DocmanOidcProperties(true, null, JWKS_URI, AUDIENCE, TENANT))
                .verify(token).isEmpty()),
            () -> assertTrue(verifier(new DocmanOidcProperties(true, ISSUER, "  ", AUDIENCE, TENANT))
                .verify(token).isEmpty()),
            () -> assertTrue(verifier(new DocmanOidcProperties(true, ISSUER, JWKS_URI, null, TENANT))
                .verify(token).isEmpty()),
            () -> assertTrue(verifier(new DocmanOidcProperties(true, ISSUER, JWKS_URI, AUDIENCE, ""))
                .verify(token).isEmpty())
        );
    }

    @Test
    void validTokenYieldsVerifiedRawIssuerWithoutBinding() throws Exception {
        Optional<VerifiedOidcToken> result = verifier(configured()).verify(validToken());

        assertAll(
            () -> assertTrue(result.isPresent()),
            () -> assertEquals(ISSUER, result.orElseThrow().issuer()),
            () -> assertEquals(SUBJECT, result.orElseThrow().subject()),
            () -> assertEquals(TENANT, result.orElseThrow().tenantId())
        );
    }

    @Test
    void canonicalIssuerKeyIsNeverAcceptedAsJwtIssuer() throws Exception {
        String token = token(CANONICAL_ISSUER, SUBJECT, List.of(AUDIENCE), AUDIENCE, future(), signingKey);

        assertTrue(verifier(configured()).verify(token).isEmpty());
    }

    @Test
    void wrongIssuerFailsClosed() throws Exception {
        String token = token("https://other-issuer.example.test", SUBJECT,
            List.of(AUDIENCE), AUDIENCE, future(), signingKey);

        assertTrue(verifier(configured()).verify(token).isEmpty());
    }

    @Test
    void wrongAudienceFailsClosed() throws Exception {
        String token = token(ISSUER, SUBJECT, List.of("another-client"), AUDIENCE, future(), signingKey);

        assertTrue(verifier(configured()).verify(token).isEmpty());
    }

    @Test
    void multipleAudiencesRequireMatchingAzp() throws Exception {
        CasdoorOidcTokenVerifier verifier = verifier(configured());
        List<String> audiences = List.of(AUDIENCE, "another-client");

        String withoutAzp = token(ISSUER, SUBJECT, audiences, null, future(), signingKey);
        String matchingAzp = token(ISSUER, SUBJECT, audiences, AUDIENCE, future(), signingKey);
        String wrongAzp = token(ISSUER, SUBJECT, audiences, "another-client", future(), signingKey);

        assertAll(
            () -> assertTrue(verifier.verify(withoutAzp).isEmpty()),
            () -> assertTrue(verifier.verify(matchingAzp).isPresent()),
            () -> assertTrue(verifier.verify(wrongAzp).isEmpty())
        );
    }

    @Test
    void singleAudienceWithMismatchedAzpFailsClosed() throws Exception {
        String token = token(ISSUER, SUBJECT, List.of(AUDIENCE), "another-client", future(), signingKey);

        assertTrue(verifier(configured()).verify(token).isEmpty());
    }

    @Test
    void expiredTokenFailsClosed() throws Exception {
        Instant issuedAt = Instant.now().minusSeconds(7200);
        Instant expiredAt = Instant.now().minusSeconds(3600);
        JWTClaimsSet claims = baseClaims(ISSUER, SUBJECT, List.of(AUDIENCE), AUDIENCE)
            .issueTime(Date.from(issuedAt))
            .expirationTime(Date.from(expiredAt))
            .build();
        String token = sign(claims, signingKey);

        assertTrue(verifier(configured()).verify(token).isEmpty());
    }

    @Test
    void invalidTokenFailsClosed() {
        CasdoorOidcTokenVerifier verifier = verifier(configured());

        assertAll(
            () -> assertTrue(verifier.verify("not-a-jwt").isEmpty()),
            () -> assertTrue(verifier.verify("a.b.c").isEmpty()),
            () -> assertTrue(verifier.verify("eyJhbGciOiJub25lIn0.e30.").isEmpty())
        );
    }

    @Test
    void tokenSignedByForeignKeyFailsClosed() throws Exception {
        String token = token(ISSUER, SUBJECT, List.of(AUDIENCE), AUDIENCE, future(), foreignKey);

        assertTrue(verifier(configured()).verify(token).isEmpty());
    }

    @Test
    void missingSubjectFailsClosedEvenWhenOtherIdentityClaimsPresent() throws Exception {
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
            .issuer(ISSUER)
            .audience(AUDIENCE)
            .claim("azp", AUDIENCE)
            .claim("username", "alice")
            .claim("userId", 99L)
            .claim("employeeId", "E-001")
            .issueTime(Date.from(Instant.now().minusSeconds(30)))
            .expirationTime(Date.from(future()))
            .build();
        String token = sign(claims, signingKey);

        assertTrue(verifier(configured()).verify(token).isEmpty());
    }

    @Test
    void subjectComesFromSubClaimNotUsernameOrEmployeeId() throws Exception {
        JWTClaimsSet claims = baseClaims(ISSUER, SUBJECT, List.of(AUDIENCE), AUDIENCE)
            .claim("username", "alice")
            .claim("userId", 99L)
            .claim("employeeId", "E-001")
            .build();
        String token = sign(claims, signingKey);

        Optional<VerifiedOidcToken> result = verifier(configured()).verify(token);

        assertAll(
            () -> assertTrue(result.isPresent()),
            () -> assertEquals(SUBJECT, result.orElseThrow().subject())
        );
    }

    private static CasdoorOidcTokenVerifier verifier(DocmanOidcProperties properties) {
        return new CasdoorOidcTokenVerifier(properties, decoder);
    }

    private static DocmanOidcProperties configured() {
        return new DocmanOidcProperties(true, ISSUER, JWKS_URI, AUDIENCE, TENANT);
    }

    private static String validToken() throws Exception {
        return token(ISSUER, SUBJECT, List.of(AUDIENCE), AUDIENCE, future(), signingKey);
    }

    private static String token(String issuer, String subject, List<String> audiences, String azp,
                                Instant expiration, RSAPrivateKey key) throws Exception {
        JWTClaimsSet.Builder builder = baseClaims(issuer, subject, audiences, azp);
        builder.expirationTime(Date.from(expiration));
        return sign(builder.build(), key);
    }

    private static JWTClaimsSet.Builder baseClaims(String issuer, String subject, List<String> audiences, String azp) {
        JWTClaimsSet.Builder builder = new JWTClaimsSet.Builder()
            .issuer(issuer)
            .audience(audiences)
            .issueTime(Date.from(Instant.now().minusSeconds(30)));
        if (subject != null) {
            builder.subject(subject);
        }
        if (azp != null) {
            builder.claim("azp", azp);
        }
        return builder;
    }

    private static String sign(JWTClaimsSet claims, RSAPrivateKey key) throws Exception {
        SignedJWT jwt = new SignedJWT(
            new JWSHeader.Builder(JWSAlgorithm.RS256).keyID("test-key").build(),
            claims
        );
        jwt.sign(new RSASSASigner(key));
        return jwt.serialize();
    }

    private static KeyPair generateKeyPair() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        return generator.generateKeyPair();
    }

    private static Instant future() {
        return Instant.now().plusSeconds(600);
    }
}
