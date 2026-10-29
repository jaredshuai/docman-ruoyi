package org.dromara.docman.config;

import com.jxccs.authz.ClientConfig;
import com.jxccs.authz.ConfigException;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Locale;

/**
 * 授权中心客户端的显式配置。
 * <p>
 * 不提供默认 mode/provider/应用/环境，也不提供任何合成身份。LOCAL + FIXTURE 只允许在
 * local 显式配置；dev/prod 的启动安全检查会拒绝合成 provider。
 */
@Data
@Component
@ConfigurationProperties(prefix = "docman.authorization")
public class DocmanAuthorizationProperties {

    private boolean enabled;

    /** LOCAL / INTEGRATION / RELEASE，必须显式配置。 */
    private String mode;

    /** FIXTURE / HTTP，必须显式配置。 */
    private String provider;

    private String application;

    private String environment;

    private String baseUrl;

    private String fixtureScenario;

    /** 运行时服务凭据，只允许来自环境变量/Secret。 */
    private String serviceCredential;

    private Duration connectTimeout = Duration.ofSeconds(5);

    private Duration requestTimeout = Duration.ofSeconds(10);

    /**
     * 是否具备可用的显式配置；任何缺失或 SDK 校验失败都返回 false，调用方必须 fail closed。
     */
    public boolean isConfigured() {
        if (!enabled) {
            return false;
        }
        try {
            ClientConfig config = toClientConfig();
            return config.provider() != ClientConfig.Provider.HTTP || notBlank(serviceCredential);
        } catch (RuntimeException ex) {
            return false;
        }
    }

    /**
     * 构造 SDK 配置；无效配置抛出 {@link ConfigException}，不静默回退。
     */
    public ClientConfig toClientConfig() {
        ClientConfig.Mode clientMode = parse(ClientConfig.Mode.class, mode, "mode");
        ClientConfig.Provider clientProvider = parse(ClientConfig.Provider.class, provider, "provider");

        if (!notBlank(application)) {
            throw new ConfigException("application must be explicitly configured");
        }
        if (!notBlank(environment)) {
            throw new ConfigException("environment must be explicitly configured");
        }

        ClientConfig.Builder builder = ClientConfig.builder()
            .mode(clientMode)
            .provider(clientProvider)
            .application(application)
            .environment(environment)
            .connectTimeout(connectTimeout)
            .requestTimeout(requestTimeout);

        if (clientProvider == ClientConfig.Provider.HTTP) {
            if (!notBlank(serviceCredential)) {
                throw new ConfigException("service credential must be provided at runtime for the http provider");
            }
            builder.baseUrl(baseUrl);
        } else {
            builder.fixtureScenario(fixtureScenario);
        }
        return builder.build();
    }

    @Override
    public String toString() {
        return "DocmanAuthorizationProperties["
            + "enabled=" + enabled
            + ", mode=" + mode
            + ", provider=" + provider
            + ", application=" + application
            + ", environment=" + environment
            + ", baseUrl=" + baseUrl
            + ", fixtureScenario=" + fixtureScenario
            + ", serviceCredential=<redacted>"
            + ", connectTimeout=" + connectTimeout
            + ", requestTimeout=" + requestTimeout
            + ']';
    }

    private static <E extends Enum<E>> E parse(Class<E> type, String value, String field) {
        if (!notBlank(value)) {
            throw new ConfigException(field + " must be explicitly configured");
        }
        try {
            return Enum.valueOf(type, value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new ConfigException(field + " is invalid");
        }
    }

    private static boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }
}
