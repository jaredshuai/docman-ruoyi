package org.dromara.docman.infrastructure.ai;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import org.dromara.docman.config.DocmanAiConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Tag("dev")
class HttpLlmGenerateAdapterTest {

    private DocmanAiConfig aiConfig;
    private HttpLlmGenerateAdapter adapter;
    private MockedStatic<HttpRequest> httpRequestMock;

    @BeforeEach
    void setUp() {
        aiConfig = new DocmanAiConfig();
        aiConfig.setApiUrl("http://localhost:11434/api/generate");
        aiConfig.setModel("qwen2.5");
        aiConfig.setTimeout(60000);
        adapter = new HttpLlmGenerateAdapter(aiConfig);
        httpRequestMock = mockStatic(HttpRequest.class);
    }

    @AfterEach
    void tearDown() {
        httpRequestMock.close();
    }

    @Test
    void generate_shouldReturnResponse_whenOkStatus() {
        // Arrange
        HttpResponse mockResponse = mock(HttpResponse.class);
        when(mockResponse.isOk()).thenReturn(true);
        when(mockResponse.body()).thenReturn("{\"response\":\"AI generated text\"}");

        HttpRequest mockRequest = mock(HttpRequest.class);
        when(mockRequest.body(anyString())).thenReturn(mockRequest);
        when(mockRequest.contentType(anyString())).thenReturn(mockRequest);
        when(mockRequest.timeout(anyInt())).thenReturn(mockRequest);
        when(mockRequest.execute()).thenReturn(mockResponse);

        httpRequestMock.when(() -> HttpRequest.post(anyString())).thenReturn(mockRequest);

        // Act
        String result = adapter.generate("test prompt", 100);

        // Assert
        assertEquals("AI generated text", result);
    }

    @Test
    void generate_shouldReturnNull_whenNotOkStatus() {
        // Arrange
        HttpResponse mockResponse = mock(HttpResponse.class);
        when(mockResponse.isOk()).thenReturn(false);
        when(mockResponse.getStatus()).thenReturn(500);

        HttpRequest mockRequest = mock(HttpRequest.class);
        when(mockRequest.body(anyString())).thenReturn(mockRequest);
        when(mockRequest.contentType(anyString())).thenReturn(mockRequest);
        when(mockRequest.timeout(anyInt())).thenReturn(mockRequest);
        when(mockRequest.execute()).thenReturn(mockResponse);

        httpRequestMock.when(() -> HttpRequest.post(anyString())).thenReturn(mockRequest);

        // Act
        String result = adapter.generate("test prompt", 100);

        // Assert
        assertNull(result);
    }
}