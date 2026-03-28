package org.dromara.docman.application.service;

import jakarta.servlet.http.HttpServletResponse;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.docman.application.port.out.DocumentStoragePort;
import org.dromara.docman.domain.entity.DocDocumentRecord;
import org.dromara.docman.service.IDocDocumentRecordService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletResponse;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("dev")
@Tag("prod")
@Tag("local")
class DocDocumentApplicationServiceTest {

    @Mock
    private IDocDocumentRecordService documentRecordService;

    @Mock
    private DocumentStoragePort documentStoragePort;

    @InjectMocks
    private DocDocumentApplicationService applicationService;

    @TempDir
    Path tempDir;

    @AfterEach
    void tearDown() {
        System.clearProperty("docman.upload.localRoot");
    }

    @Test
    void shouldFallbackToLocalFileWhenOssLoadFails() throws Exception {
        System.setProperty("docman.upload.localRoot", tempDir.toString());
        Path localFile = tempDir.resolve("demo").resolve("file.txt");
        Files.createDirectories(localFile.getParent());
        byte[] expected = "fallback-content".getBytes(StandardCharsets.UTF_8);
        Files.write(localFile, expected);

        DocDocumentRecord record = new DocDocumentRecord();
        record.setNasPath("/demo/file.txt");
        record.setFileName("file.txt");
        when(documentRecordService.queryEntityById(anyLong())).thenReturn(record);
        when(documentStoragePort.load("/demo/file.txt")).thenThrow(new IllegalStateException("oss unavailable"));

        MockHttpServletResponse response = new MockHttpServletResponse();

        applicationService.download(1L, response);

        assertArrayEquals(expected, response.getContentAsByteArray());
        assertEquals(expected.length, response.getContentLength());
    }

    @Test
    void shouldRejectTraversalPathWhenFallbackEscapesLocalRoot() {
        System.setProperty("docman.upload.localRoot", tempDir.toString());

        DocDocumentRecord record = new DocDocumentRecord();
        record.setNasPath("../../secret.txt");
        record.setFileName("secret.txt");
        when(documentRecordService.queryEntityById(anyLong())).thenReturn(record);
        when(documentStoragePort.load("../../secret.txt")).thenThrow(new IllegalStateException("oss unavailable"));

        MockHttpServletResponse response = new MockHttpServletResponse();

        ServiceException ex = assertThrows(ServiceException.class, () -> applicationService.download(2L, response));
        assertEquals("文档内容读取失败", ex.getMessage());
    }

    @Test
    void shouldRejectEmptyPathBeforeLoading() {
        DocDocumentRecord record = new DocDocumentRecord();
        record.setNasPath(" ");
        when(documentRecordService.queryEntityById(anyLong())).thenReturn(record);

        MockHttpServletResponse response = new MockHttpServletResponse();

        ServiceException ex = assertThrows(ServiceException.class, () -> applicationService.download(3L, response));
        assertEquals("文档内容不可用", ex.getMessage());
    }

    @Test
    void shouldSanitizeMissingFallbackFileError() {
        System.setProperty("docman.upload.localRoot", tempDir.toString());

        DocDocumentRecord record = new DocDocumentRecord();
        record.setNasPath("/missing/secret.txt");
        record.setFileName("secret.txt");
        when(documentRecordService.queryEntityById(anyLong())).thenReturn(record);
        when(documentStoragePort.load("/missing/secret.txt")).thenThrow(new IllegalStateException("oss unavailable"));

        MockHttpServletResponse response = new MockHttpServletResponse();

        ServiceException ex = assertThrows(ServiceException.class, () -> applicationService.download(4L, response));
        assertEquals("文档内容读取失败", ex.getMessage());
        org.junit.jupiter.api.Assertions.assertFalse(ex.getMessage().contains("secret.txt"));
        org.junit.jupiter.api.Assertions.assertFalse(ex.getMessage().contains(tempDir.toString()));
    }
}
