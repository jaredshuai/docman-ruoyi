package org.dromara.docman.domain.service;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Map;

@Component
public class DocPathResolver {

    private static final Map<String, String> CUSTOMER_TYPE_MAP = Map.of(
        "telecom", "电信",
        "social", "社会客户"
    );

    public String buildProjectBasePath(String customerType, String projectName) {
        int year = LocalDate.now().getYear();
        String customerLabel = CUSTOMER_TYPE_MAP.getOrDefault(customerType, customerType);
        return String.format("/项目文档/%d/%s/%s", year, customerLabel, projectName);
    }
}
