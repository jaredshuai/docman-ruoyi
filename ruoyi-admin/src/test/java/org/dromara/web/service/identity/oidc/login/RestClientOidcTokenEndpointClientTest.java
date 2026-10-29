package org.dromara.web.service.identity.oidc.login;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.io.IOException;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@Tag("dev")
@Tag("prod")
@Tag("local")
class RestClientOidcTokenEndpointClientTest {

    private static final String TOKEN_URI = "https://casdoor.example.test/api/login/oauth/access_token";

    private MockRestServiceServer server;
    private RestClientOidcTokenEndpointClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new RestClientOidcTokenEndpointClient(properties(TOKEN_URI), builder.build());
    }

    @Test
    void exchangesCodeWithPkceAndReturnsIdToken() {
        server.expect(requestTo(TOKEN_URI))
            .andExpect(method(org.springframework.http.HttpMethod.POST))
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_FORM_URLENCODED))
            .andExpect(content().string(containsString("grant_type=authorization_code")))
            .andExpect(content().string(containsString("code=code-1")))
            .andExpect(content().string(containsString("client_id=docman-client")))
            .andExpect(content().string(containsString("client_secret=runtime-secret")))
            .andExpect(content().string(containsString("code_verifier=verifier-1")))
            .andRespond(withSuccess("{\"id_token\":\"header.payload.signature\"}", MediaType.APPLICATION_JSON));

        String idToken = client.exchangeIdToken(request());

        assertEquals("header.payload.signature", idToken);
        server.verify();
    }

    @Test
    void missingIdTokenIsMappedTo503() {
        server.expect(requestTo(TOKEN_URI))
            .andRespond(withSuccess("{\"token_type\":\"Bearer\"}", MediaType.APPLICATION_JSON));

        OidcLoginException ex = assertThrows(OidcLoginException.class,
            () -> client.exchangeIdToken(request()));

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, ex.status());
    }

    @Test
    void blankIdTokenIsMappedTo503() {
        server.expect(requestTo(TOKEN_URI))
            .andRespond(withSuccess("{\"id_token\":\"   \"}", MediaType.APPLICATION_JSON));

        OidcLoginException ex = assertThrows(OidcLoginException.class,
            () -> client.exchangeIdToken(request()));

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, ex.status());
    }

    @Test
    void providerErrorIsMappedTo503() {
        server.expect(requestTo(TOKEN_URI))
            .andRespond(withStatus(HttpStatus.BAD_GATEWAY));

        OidcLoginException ex = assertThrows(OidcLoginException.class,
            () -> client.exchangeIdToken(request()));

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, ex.status());
    }

    @Test
    void networkFailureIsMappedTo503() {
        server.expect(requestTo(TOKEN_URI))
            .andRespond(request -> {
                throw new IOException("connection reset");
            });

        OidcLoginException ex = assertThrows(OidcLoginException.class,
            () -> client.exchangeIdToken(request()));

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, ex.status());
    }

    @Test
    void unconfiguredTokenUriFailsClosedWithoutHttpCall() {
        RestClientOidcTokenEndpointClient unconfigured = new RestClientOidcTokenEndpointClient(
            properties(""), RestClient.create());

        OidcLoginException ex = assertThrows(OidcLoginException.class,
            () -> unconfigured.exchangeIdToken(request()));

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, ex.status());
    }

    @Test
    void incompleteExchangeRequestFailsClosed() {
        OidcTokenExchangeRequest missingVerifier = new OidcTokenExchangeRequest(
            "code-1", "https://docman.example.test/auth/oidc/callback", "docman-client", "runtime-secret", " ");

        OidcLoginException ex = assertThrows(OidcLoginException.class,
            () -> client.exchangeIdToken(missingVerifier));

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, ex.status());
    }

    private static OidcTokenExchangeRequest request() {
        return new OidcTokenExchangeRequest(
            "code-1",
            "https://docman.example.test/auth/oidc/callback",
            "docman-client",
            "runtime-secret",
            "verifier-1"
        );
    }

    private static DocmanOidcLoginProperties properties(String tokenUri) {
        return new DocmanOidcLoginProperties(
            true,
            "https://casdoor.example.test",
            "casdoor-jiaxing-company",
            "https://casdoor.example.test/.well-known/jwks",
            "docman-client",
            "docman-client",
            "runtime-secret",
            "https://casdoor.example.test/login/oauth/authorize",
            tokenUri,
            "https://docman.example.test/auth/oidc/callback",
            "000000",
            "openid",
            null,
            null,
            null
        );
    }
}
