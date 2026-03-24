package org.dromara.docman.domain.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@Tag("dev")
@Tag("prod")
@Tag("local")
class DocPathResolverTest {

    private final DocPathResolver resolver = new DocPathResolver();
    private final int currentYear = LocalDate.now().getYear();

    @Test
    void shouldMapTelecomToChinese() {
        String path = resolver.buildProjectBasePath("telecom", "测试项目");
        assertEquals(String.format("/项目文档/%d/电信/测试项目", currentYear), path);
    }

    @Test
    void shouldMapSocialToChinese() {
        String path = resolver.buildProjectBasePath("social", "社会项目");
        assertEquals(String.format("/项目文档/%d/社会客户/社会项目", currentYear), path);
    }

    @Test
    void shouldFallbackForUnknownCustomerType() {
        String path = resolver.buildProjectBasePath("unknown_type", "其他项目");
        assertEquals(String.format("/项目文档/%d/unknown_type/其他项目", currentYear), path);
    }

    @Test
    void shouldThrowNPEForNullCustomerType() {
        assertThrows(NullPointerException.class, () ->
            resolver.buildProjectBasePath(null, "空类型项目"));
    }
}