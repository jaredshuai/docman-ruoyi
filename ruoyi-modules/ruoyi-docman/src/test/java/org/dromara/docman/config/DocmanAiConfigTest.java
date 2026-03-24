package org.dromara.docman.config;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Tag("dev")
@Tag("prod")
@Tag("local")
class DocmanAiConfigTest {

    @Test
    void shouldHaveDefaultValues() {
        DocmanAiConfig config = new DocmanAiConfig();

        assertEquals("http://localhost:11434/api/generate", config.getApiUrl());
        assertEquals("qwen2.5", config.getModel());
        assertEquals(60000, config.getTimeout());
        assertEquals(4096, config.getMaxTokens());
    }

    @Test
    void shouldSetAndGetApiUrl() {
        DocmanAiConfig config = new DocmanAiConfig();

        config.setApiUrl("http://custom-host:8080/api");

        assertEquals("http://custom-host:8080/api", config.getApiUrl());
    }

    @Test
    void shouldSetAndGetModel() {
        DocmanAiConfig config = new DocmanAiConfig();

        config.setModel("gpt-4");

        assertEquals("gpt-4", config.getModel());
    }

    @Test
    void shouldSetAndGetTimeout() {
        DocmanAiConfig config = new DocmanAiConfig();

        config.setTimeout(30000);

        assertEquals(30000, config.getTimeout());
    }

    @Test
    void shouldSetAndGetMaxTokens() {
        DocmanAiConfig config = new DocmanAiConfig();

        config.setMaxTokens(8192);

        assertEquals(8192, config.getMaxTokens());
    }
}