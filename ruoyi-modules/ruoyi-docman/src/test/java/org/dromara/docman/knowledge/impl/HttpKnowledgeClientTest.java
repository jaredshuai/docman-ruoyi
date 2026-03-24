package org.dromara.docman.knowledge.impl;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import org.dromara.docman.knowledge.KnowledgeClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class HttpKnowledgeClientTest {

    private HttpKnowledgeClient client;
    private MockedStatic<HttpRequest> mockedStatic;

    @BeforeEach
    void setUp() throws Exception {
        client = new HttpKnowledgeClient();
        setField(client, "baseUrl", "http://localhost:8000");
        setField(client, "timeout", 30000);
        mockedStatic = mockStatic(HttpRequest.class);
    }

    @AfterEach
    void tearDown() {
        if (mockedStatic != null) {
            mockedStatic.close();
        }
    }

    @Test
    void search_successfulResponse_mapsContentAndSource() {
        // Arrange
        HttpRequest mockRequest = mock(HttpRequest.class);
        HttpResponse mockResponse = mock(HttpResponse.class);

        mockedStatic.when(() -> HttpRequest.post("http://localhost:8000/knowledge/search"))
            .thenReturn(mockRequest);

        when(mockRequest.body(anyString())).thenReturn(mockRequest);
        when(mockRequest.contentType(anyString())).thenReturn(mockRequest);
        when(mockRequest.timeout(anyInt())).thenReturn(mockRequest);
        when(mockRequest.execute()).thenReturn(mockResponse);
        when(mockResponse.isOk()).thenReturn(true);
        when(mockResponse.body()).thenReturn("{\"results\":[{\"content\":\"test content\",\"source\":\"test source\"},{\"content\":\"content2\",\"source\":\"source2\"}]}");

        // Act
        var results = client.search("query", 5);

        // Assert
        assertEquals(2, results.size());
        assertEquals("test content", results.get(0).content());
        assertEquals("test source", results.get(0).source());
        assertEquals("content2", results.get(1).content());
        assertEquals("source2", results.get(1).source());
    }

    @Test
    void search_nonOkResponse_returnsEmptyList() {
        // Arrange
        HttpRequest mockRequest = mock(HttpRequest.class);
        HttpResponse mockResponse = mock(HttpResponse.class);

        mockedStatic.when(() -> HttpRequest.post("http://localhost:8000/knowledge/search"))
            .thenReturn(mockRequest);

        when(mockRequest.body(anyString())).thenReturn(mockRequest);
        when(mockRequest.contentType(anyString())).thenReturn(mockRequest);
        when(mockRequest.timeout(anyInt())).thenReturn(mockRequest);
        when(mockRequest.execute()).thenReturn(mockResponse);
        when(mockResponse.isOk()).thenReturn(false);
        when(mockResponse.getStatus()).thenReturn(500);

        // Act
        var results = client.search("query", 5);

        // Assert
        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    void search_okResponseMissingResultsArray_returnsEmptyList() {
        // Arrange
        HttpRequest mockRequest = mock(HttpRequest.class);
        HttpResponse mockResponse = mock(HttpResponse.class);

        mockedStatic.when(() -> HttpRequest.post("http://localhost:8000/knowledge/search"))
            .thenReturn(mockRequest);

        when(mockRequest.body(anyString())).thenReturn(mockRequest);
        when(mockRequest.contentType(anyString())).thenReturn(mockRequest);
        when(mockRequest.timeout(anyInt())).thenReturn(mockRequest);
        when(mockRequest.execute()).thenReturn(mockResponse);
        when(mockResponse.isOk()).thenReturn(true);
        when(mockResponse.body()).thenReturn("{\"message\":\"no results\"}");

        // Act
        var results = client.search("query", 5);

        // Assert
        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    void search_exceptionDuringRequest_returnsEmptyList() {
        // Arrange
        HttpRequest mockRequest = mock(HttpRequest.class);

        mockedStatic.when(() -> HttpRequest.post("http://localhost:8000/knowledge/search"))
            .thenReturn(mockRequest);

        when(mockRequest.body(anyString())).thenReturn(mockRequest);
        when(mockRequest.contentType(anyString())).thenReturn(mockRequest);
        when(mockRequest.timeout(anyInt())).thenReturn(mockRequest);
        when(mockRequest.execute()).thenThrow(new RuntimeException("Connection refused"));

        // Act
        var results = client.search("query", 5);

        // Assert
        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    void search_exceptionDuringJsonHandling_returnsEmptyList() {
        // Arrange
        HttpRequest mockRequest = mock(HttpRequest.class);
        HttpResponse mockResponse = mock(HttpResponse.class);

        mockedStatic.when(() -> HttpRequest.post("http://localhost:8000/knowledge/search"))
            .thenReturn(mockRequest);

        when(mockRequest.body(anyString())).thenReturn(mockRequest);
        when(mockRequest.contentType(anyString())).thenReturn(mockRequest);
        when(mockRequest.timeout(anyInt())).thenReturn(mockRequest);
        when(mockRequest.execute()).thenReturn(mockResponse);
        when(mockResponse.isOk()).thenReturn(true);
        when(mockResponse.body()).thenReturn("invalid json {{{");

        // Act
        var results = client.search("query", 5);

        // Assert
        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    private static void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}