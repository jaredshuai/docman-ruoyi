package org.dromara.web.service.identity.oidc.login;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.Map;

/**
 * 使用 Spring {@link RestClient} 完成 authorization-code + PKCE token 交换。
 * <p>
 * 任何非 2xx、缺失 id_token、网络错误或超时都映射为 503；不记录也不返回
 * client secret、authorization code、code verifier 或 provider 响应体。
 */
public class RestClientOidcTokenEndpointClient implements OidcTokenEndpointClient {

    private final DocmanOidcLoginProperties properties;
    private final RestClient restClient;

    public RestClientOidcTokenEndpointClient(DocmanOidcLoginProperties properties, RestClient restClient) {
        this.properties = properties;
        this.restClient = restClient;
    }

    @Override
    public String exchangeIdToken(OidcTokenExchangeRequest request) {
        if (properties == null || properties.tokenUri() == null || properties.tokenUri().isBlank()) {
            throw OidcLoginException.unavailable("OIDC token endpoint is not configured");
        }
        require(request.code());
        require(request.redirectUri());
        require(request.clientId());
        require(request.clientSecret());
        require(request.codeVerifier());

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code");
        form.add("code", request.code());
        form.add("redirect_uri", request.redirectUri());
        form.add("client_id", request.clientId());
        form.add("client_secret", request.clientSecret());
        form.add("code_verifier", request.codeVerifier());

        try {
            Map<String, Object> body = restClient.post()
                .uri(properties.tokenUri())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .accept(MediaType.APPLICATION_JSON)
                .body(form)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });
            Object idToken = body == null ? null : body.get("id_token");
            if (!(idToken instanceof String token) || token.isBlank()) {
                throw OidcLoginException.unavailable("OIDC token endpoint returned no ID token");
            }
            return token;
        } catch (OidcLoginException ex) {
            throw ex;
        } catch (RestClientResponseException ex) {
            throw OidcLoginException.unavailable("OIDC token endpoint rejected the exchange");
        } catch (ResourceAccessException ex) {
            throw OidcLoginException.unavailable("OIDC token endpoint is unavailable");
        } catch (RuntimeException ex) {
            throw OidcLoginException.unavailable("OIDC token endpoint protocol error");
        }
    }

    private static void require(String value) {
        if (value == null || value.isBlank()) {
            throw OidcLoginException.unavailable("OIDC token exchange request is incomplete");
        }
    }
}
