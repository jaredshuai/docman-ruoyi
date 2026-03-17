package org.dromara.docman.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "docman.job")
public class DocmanJobConfig {

    /**
     * 文档待生成超过该天数后触发提醒。
     */
    private int documentReminderPendingDays = 3;
}
