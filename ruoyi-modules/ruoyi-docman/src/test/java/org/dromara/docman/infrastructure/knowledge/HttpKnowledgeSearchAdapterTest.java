package org.dromara.docman.infrastructure.knowledge;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import org.dromara.docman.application.port.out.KnowledgeSearchPort;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * HttpKnowledgeSearchAdapter 单元测试
 */
@Tag("local")
class HttpKnowledgeSearchAdapterTest {

    private HttpKnowledgeSearchAdapter adapter;
    private MockedStatic<HttpRequest> httpRequestMock;
    private HttpRequest mockRequest;
    private HttpResponse mockResponse;

    @BeforeEach
    void setUp() {
        adapter = new HttpKnowledgeSearchAdapter();
        ReflectionTestUtils.setField(adapter, "baseUrl", "http://localhost:8000");
        ReflectionTestUtils.setField(adapter, "timeout", 30000);

        httpRequestMock = Mockito.mockStatic(HttpRequest.class);
        mockRequest = mock(HttpRequest.class);
        mockResponse = mock(HttpResponse.class);
    }

    @AfterEach
    void tearDown() {
        httpRequestMock.close();
    }

    @Test
    void search_shouldReturnEmptyList_whenNon200Response() {
        // Arrange
        httpRequestMock.when(() -> HttpRequest.post(anyString())).thenReturn(mockRequest);
        when(mockRequest.body(anyString())).thenReturn(mockRequest);
        when(mockRequest.contentType(anyString())).thenReturn(mockRequest);
        when(mockRequest.timeout(anyInt())).thenReturn(mockRequest);
        when(mockRequest.execute()).thenReturn(mockResponse);
        when(mockResponse.isOk()).thenReturn(false);
        when(mockResponse.getStatus()).thenReturn(500);

        // Act
        List<KnowledgeSearchPort.KnowledgeResult> result = adapter.search("test query", 5);

        // Assert
        assertTrue(result.isEmpty());
        verify(mockResponse).getStatus();
    }

    @Test
    void search_shouldParseValidResults_whenOkResponse() {
        // Arrange
        String jsonBody = "{\"results\":[{\"content\":\"content1\",\"source\":\"source1\"},{\"content\":\"content2\",\"source\":\"source2\"}]}";
        httpRequestMock.when(() -> HttpRequest.post(anyString())).thenReturn(mockRequest);
        when(mockRequest.body(anyString())).thenReturn(mockRequest);
        when(mockRequest.contentType(anyString())).thenReturn(mockRequest);
        when(mockRequest.timeout(anyInt())).thenReturn(mockRequest);
        when(mockRequest.execute()).thenReturn(mockResponse);
        when(mockResponse.isOk()).thenReturn(true);
        when(mockResponse.body()).thenReturn(jsonBody);

        // Act
        List<KnowledgeSearchPort.KnowledgeResult> result = adapter.search("test query", 5);

        // Assert
        assertEquals(2, result.size());
        assertEquals("content1", result.get(0).content());
        assertEquals("source1", result.get(0).source());
        assertEquals("content2", result.get(1).content());
        assertEquals("source2", result.get(1).source());
    }

    @Test
    void search_shouldReturnEmptyList_whenMissingResultsArray() {
        // Arrange
        String jsonBody = "{\"error\":\"no results\"}";
        httpRequestMock.when(() -> HttpRequest.post(anyString())).thenReturn(mockRequest);
        when(mockRequest.body(anyString())).thenReturn(mockRequest);
        when(mockRequest.contentType(anyString())).thenReturn(mockRequest);
        when(mockRequest.timeout(anyInt())).thenReturn(mockRequest);
        when(mockRequest.execute()).thenReturn(mockResponse);
        when(mockResponse.isOk()).thenReturn(true);
        when(mockResponse.body()).thenReturn(jsonBody);

        // Act
        List<KnowledgeSearchPort.KnowledgeResult> result = adapter.search("test query", 5);

        // Assert
        assertTrue(result.isEmpty());
    }

    @Test
    void search_shouldReturnEmptyList_whenExceptionThrown() {
        // Arrange
        httpRequestMock.when(() -> HttpRequest.post(anyString())).thenThrow(new RuntimeException("Connection refused"));

        // Act
        List<KnowledgeSearchPort.KnowledgeResult> result = adapter.search("test query", 5);

        // Assert
        assertTrue(result.isEmpty());
    }
}