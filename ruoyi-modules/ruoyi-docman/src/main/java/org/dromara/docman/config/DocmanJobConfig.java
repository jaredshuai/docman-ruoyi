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

    /**
     * 节点截止日期提前多少天开始提醒。
     */
    private int reminderAdvanceDays = 3;

    /**
     * 节点截止提醒最大次数。
     */
    private int maxReminderCount = 5;
}
