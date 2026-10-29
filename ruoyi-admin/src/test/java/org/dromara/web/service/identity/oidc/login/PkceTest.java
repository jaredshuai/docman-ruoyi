package org.dromara.web.service.identity.oidc.login;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("dev")
@Tag("prod")
@Tag("local")
class PkceTest {

    /** RFC 7636 附录 B 的公开测试向量。 */
    private static final String RFC_VERIFIER = "dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk";
    private static final String RFC_CHALLENGE = "E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM";

    @Test
    void codeVerifierMatchesRfc7636CharacterSetAndLength() {
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < 32; i++) {
            String verifier = Pkce.newCodeVerifier();
            assertEquals(43, verifier.length());
            assertTrue(verifier.matches("[A-Za-z0-9_-]+"));
            assertTrue(seen.add(verifier));
        }
    }

    @Test
    void stateAndNonceAreRandomAndNonBlank() {
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < 16; i++) {
            String state = Pkce.newState();
            String nonce = Pkce.newNonce();
            assertFalse(state.isBlank());
            assertFalse(nonce.isBlank());
            assertNotEquals(state, nonce);
            assertTrue(seen.add(state));
            assertTrue(seen.add(nonce));
        }
    }

    @Test
    void challengeMatchesRfc7636Vector() {
        assertEquals(RFC_CHALLENGE, Pkce.challenge(RFC_VERIFIER));
    }

    @Test
    void challengeRejectsBlankVerifier() {
        assertThrows(IllegalArgumentException.class, () -> Pkce.challenge(null));
        assertThrows(IllegalArgumentException.class, () -> Pkce.challenge("  "));
    }
}
