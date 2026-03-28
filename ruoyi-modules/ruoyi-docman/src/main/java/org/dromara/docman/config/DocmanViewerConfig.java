package org.dromara.docman.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "docman.viewer")
public class DocmanViewerConfig {

    private boolean enabled = true;

    private String baseUrl;

    private long ticketTtlSeconds = 300;
}
