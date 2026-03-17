package org.dromara.docman.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "docman.ai")
public class DocmanAiConfig {

    private String apiUrl = "http://localhost:11434/api/generate";

    private String model = "qwen2.5";

    private int timeout = 60000;

    private int maxTokens = 4096;
}
