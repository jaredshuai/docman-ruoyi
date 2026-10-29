package org.dromara.docman.infrastructure.authz;

import com.jxccs.authz.AuthorizationClient;
import jakarta.annotation.PreDestroy;
import org.dromara.docman.config.DocmanAuthorizationProperties;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * 授权中心客户端持有者。
 * <p>
 * 未启用或配置无效时不创建客户端，调用方必须返回 503；绝不回退到本地角色或放行。
 */
@Component
public class DocmanAuthorizationClientProvider implements AutoCloseable {

    private final DocmanAuthorizationProperties properties;
    private final AuthorizationClient client;
    private final String configurationError;

    public DocmanAuthorizationClientProvider(DocmanAuthorizationProperties properties) {
        this.properties = properties;
        AuthorizationClient built = null;
        String error = null;
        if (properties != null && properties.isEnabled()) {
            try {
                built = AuthorizationClient.build(
                    properties.toClientConfig(),
                    properties::getServiceCredential
                );
            } catch (RuntimeException ex) {
                error = "authorization service configuration is invalid";
            }
        }
        this.client = built;
        this.configurationError = error;
    }

    public Optional<AuthorizationClient> client() {
        return Optional.ofNullable(client);
    }

    public String configurationError() {
        return configurationError;
    }

    @PreDestroy
    @Override
    public void close() {
        if (client != null) {
            client.close();
        }
    }
}
