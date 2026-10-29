package org.dromara.web.controller;

import org.dromara.web.service.SysLoginService;
import org.dromara.web.service.identity.oidc.login.OidcLoginException;
import org.dromara.web.service.identity.oidc.login.OidcLoginResult;
import org.dromara.web.service.identity.oidc.login.OidcLoginService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@Tag("dev")
@Tag("prod")
@Tag("local")
class OidcLoginControllerTest {

    private static final String AUTHORIZATION_URL =
        "https://casdoor.example.test/login/oauth/authorize?response_type=code&state=s1";

    @Mock
    private OidcLoginService loginService;
    @Mock
    private SysLoginService sysLoginService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        OidcLoginController controller = new OidcLoginController(loginService, sysLoginService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(new OidcLoginExceptionHandler())
            .build();
    }

    @Test
    void startRedirectsToAuthorizationEndpoint() throws Exception {
        when(loginService.buildAuthorizationUrl()).thenReturn(AUTHORIZATION_URL);

        mockMvc.perform(get("/auth/oidc/login/start"))
            .andExpect(status().isFound())
            .andExpect(header().string("Location", AUTHORIZATION_URL));
    }

    @Test
    void startMapsMissingConfigurationTo503() throws Exception {
        when(loginService.buildAuthorizationUrl())
            .thenThrow(OidcLoginException.unavailable("OIDC login is not configured"));

        mockMvc.perform(get("/auth/oidc/login/start"))
            .andExpect(status().isServiceUnavailable())
            .andExpect(jsonPath("$.code").value(503));
    }

    @Test
    void callbackReturnsLocalSessionWithoutLeakingTokenInUrl() throws Exception {
        when(loginService.callback("code-1", "state-1", null))
            .thenReturn(new OidcLoginResult("local-token", "docman-client", "openid"));

        mockMvc.perform(get("/auth/oidc/callback").param("code", "code-1").param("state", "state-1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.access_token").value("local-token"))
            .andExpect(jsonPath("$.data.client_id").value("docman-client"))
            .andExpect(jsonPath("$.data.scope").value("openid"))
            .andExpect(jsonPath("$.data.id_token").doesNotExist())
            .andExpect(jsonPath("$.data.canonical_issuer").doesNotExist());
    }

    @Test
    void callbackMapsStateFailureTo401() throws Exception {
        when(loginService.callback(null, "replayed", null))
            .thenThrow(OidcLoginException.unauthenticated("OIDC state is unknown, expired, or already used"));

        mockMvc.perform(get("/auth/oidc/callback").param("state", "replayed"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void callbackMapsBindingDenialTo403() throws Exception {
        when(loginService.callback("code-1", "state-1", null))
            .thenThrow(OidcLoginException.forbidden("OIDC identity is not bound to exactly one enabled local user"));

        mockMvc.perform(get("/auth/oidc/callback").param("code", "code-1").param("state", "state-1"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    void callbackMapsProviderFailureTo503() throws Exception {
        when(loginService.callback("code-1", "state-1", "access_denied"))
            .thenThrow(OidcLoginException.unavailable("OIDC provider returned an authorization error"));

        mockMvc.perform(get("/auth/oidc/callback")
                .param("code", "code-1")
                .param("state", "state-1")
                .param("error", "access_denied"))
            .andExpect(status().isServiceUnavailable())
            .andExpect(jsonPath("$.code").value(503));
    }

    @Test
    void logoutOnlyClearsLocalSession() throws Exception {
        mockMvc.perform(post("/auth/oidc/logout"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200));

        verify(sysLoginService).logout();
    }
}
