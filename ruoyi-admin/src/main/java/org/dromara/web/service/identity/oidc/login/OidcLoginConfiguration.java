package org.dromara.web.service.identity.oidc.login;

import org.dromara.web.service.identity.oidc.CasdoorOidcTokenVerifier;
import org.redisson.api.RedissonClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * Casdoor OIDC 登录链路的显式装配。
 * <p>
 * 所有外部地址、client id、client secret、tenant 与回调地址均来自
 * {@link DocmanOidcLoginProperties}；未配置时不构造任何默认身份或 mock provider。
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(DocmanOidcLoginProperties.class)
public class OidcLoginConfiguration {

    @Bean
    public CasdoorOidcTokenVerifier oidcTokenVerifier(DocmanOidcLoginProperties properties) {
        return new CasdoorOidcTokenVerifier(properties.toVerifierProperties());
    }

    @Bean
    public OidcLoginTransactionStore oidcLoginTransactionStore(RedissonClient redissonClient) {
        return new RedisOidcLoginTransactionStore(redissonClient);
    }

    @Bean
    public RestClient oidcTokenRestClient(RestClient.Builder builder, DocmanOidcLoginProperties properties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout((int) properties.connectTimeout().toMillis());
        requestFactory.setReadTimeout((int) properties.requestTimeout().toMillis());
        return builder.requestFactory(requestFactory).build();
    }

    @Bean
    public OidcTokenEndpointClient oidcTokenEndpointClient(RestClient oidcTokenRestClient,
                                                           DocmanOidcLoginProperties properties) {
        return new RestClientOidcTokenEndpointClient(properties, oidcTokenRestClient);
    }

    @Bean
    public OidcSessionAuthenticator oidcSessionAuthenticator() {
        return new SaTokenOidcSessionAuthenticator();
    }
}
