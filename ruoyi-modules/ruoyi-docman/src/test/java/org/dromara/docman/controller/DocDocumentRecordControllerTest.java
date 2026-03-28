package org.dromara.docman.controller;

import org.dromara.common.core.domain.R;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.docman.application.port.out.DocumentStoragePort;
import org.dromara.docman.application.service.DocDocumentApplicationService;
import org.dromara.docman.application.service.DocDocumentQueryApplicationService;
import org.dromara.docman.application.service.DocDocumentViewerApplicationService;
import org.dromara.docman.application.service.DocProjectQueryApplicationService;
import org.dromara.docman.domain.bo.DocDocumentRecordBo;
import org.dromara.docman.domain.enums.DocDocumentSourceType;
import org.dromara.docman.domain.vo.DocDocumentRecordVo;
import org.dromara.docman.domain.vo.DocProjectVo;
import org.dromara.docman.domain.vo.DocViewerTicketVo;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("dev")
@Tag("prod")
@Tag("local")
class DocDocumentRecordControllerTest {

    @Mock
    private DocDocumentApplicationService documentApplicationService;

    @Mock
    private DocDocumentQueryApplicationService documentQueryApplicationService;

    @Mock
    private DocDocumentViewerApplicationService documentViewerApplicationService;

    @Mock
    private DocProjectQueryApplicationService projectQueryApplicationService;

    @Mock
    private DocumentStoragePort documentStoragePort;

    @Test
    void shouldDelegateDocumentListQuery() {
        DocDocumentRecordController controller = new DocDocumentRecordController(
            documentApplicationService, documentQueryApplicationService, documentViewerApplicationService, projectQueryApplicationService, documentStoragePort
        );
        PageQuery pageQuery = new PageQuery(10, 1);
        TableDataInfo<DocDocumentRecordVo> expected = TableDataInfo.build(List.of(new DocDocumentRecordVo()));
        when(documentQueryApplicationService.list(5L, pageQuery)).thenReturn(expected);

        TableDataInfo<DocDocumentRecordVo> result = controller.list(5L, pageQuery);

        assertEquals(expected, result);
    }

    @Test
    void shouldRejectEmptyUploadFile() {
        DocDocumentRecordController controller = new DocDocumentRecordController(
            documentApplicationService, documentQueryApplicationService, documentViewerApplicationService, projectQueryApplicationService, documentStoragePort
        );
        MockMultipartFile file = new MockMultipartFile("file", "", "application/octet-stream", new byte[0]);

        ServiceException ex = assertThrows(ServiceException.class, () -> controller.upload(file, 1L));

        assertEquals("上传文件不能为空", ex.getMessage());
        verify(documentApplicationService, never()).upload(any(DocDocumentRecordBo.class));
    }

    @Test
    void shouldRejectUploadWhenFileExceedsLimit() {
        DocDocumentRecordController controller = new DocDocumentRecordController(
            documentApplicationService, documentQueryApplicationService, documentViewerApplicationService, projectQueryApplicationService, documentStoragePort
        );
        MockMultipartFile file = new MockMultipartFile("file", "big.pdf", "application/pdf", new byte[1]);
        org.springframework.web.multipart.MultipartFile oversized = new org.springframework.web.multipart.MultipartFile() {
            @Override
            public String getName() {
                return file.getName();
            }

            @Override
            public String getOriginalFilename() {
                return file.getOriginalFilename();
            }

            @Override
            public String getContentType() {
                return file.getContentType();
            }

            @Override
            public boolean isEmpty() {
                return false;
            }

            @Override
            public long getSize() {
                return 100L * 1024 * 1024 + 1;
            }

            @Override
            public byte[] getBytes() {
                return new byte[1];
            }

            @Override
            public java.io.InputStream getInputStream() {
                return new java.io.ByteArrayInputStream(new byte[1]);
            }

            @Override
            public void transferTo(java.io.File dest) {
            }

            @Override
            public void transferTo(java.nio.file.Path dest) {
            }
        };

        ServiceException ex = assertThrows(ServiceException.class, () -> controller.upload(oversized, 1L));

        assertEquals("上传文件不能超过 100MB", ex.getMessage());
    }

    @Test
    void shouldRejectUnsupportedFileExtension() {
        DocDocumentRecordController controller = new DocDocumentRecordController(
            documentApplicationService, documentQueryApplicationService, documentViewerApplicationService, projectQueryApplicationService, documentStoragePort
        );
        MockMultipartFile file = new MockMultipartFile("file", "script.exe", "application/octet-stream", "x".getBytes(StandardCharsets.UTF_8));

        ServiceException ex = assertThrows(ServiceException.class, () -> controller.upload(file, 1L));

        assertEquals("不支持的文件类型: .exe", ex.getMessage());
    }

    @Test
    void shouldRejectUploadWhenProjectDoesNotExist() {
        DocDocumentRecordController controller = new DocDocumentRecordController(
            documentApplicationService, documentQueryApplicationService, documentViewerApplicationService, projectQueryApplicationService, documentStoragePort
        );
        MockMultipartFile file = new MockMultipartFile("file", "report.pdf", "application/pdf", "demo".getBytes(StandardCharsets.UTF_8));
        when(projectQueryApplicationService.getById(1L)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class, () -> controller.upload(file, 1L));

        assertEquals("项目不存在", ex.getMessage());
    }

    @Test
    void shouldUploadDocumentAndPersistRecord() {
        DocDocumentRecordController controller = new DocDocumentRecordController(
            documentApplicationService, documentQueryApplicationService, documentViewerApplicationService, projectQueryApplicationService, documentStoragePort
        );
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "C:\\temp\\report.pdf",
            "application/pdf",
            "demo-pdf".getBytes(StandardCharsets.UTF_8)
        );
        DocProjectVo project = new DocProjectVo();
        project.setId(9L);
        project.setNasBasePath("/项目文档/2026/电信/演示项目");
        when(projectQueryApplicationService.getById(9L)).thenReturn(project);
        when(documentStoragePort.ensureDirectory("/项目文档/2026/电信/演示项目/uploads")).thenReturn(true);
        when(documentStoragePort.store(anyString(), any(byte[].class), anyString(), anyString()))
            .thenReturn(new DocumentStoragePort.StoredDocument("/stored/report.pdf", "stored-report.pdf", 88L));

        R<Void> result = controller.upload(file, 9L);

        assertEquals(R.SUCCESS, result.getCode());
        ArgumentCaptor<String> pathCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<byte[]> contentCaptor = ArgumentCaptor.forClass(byte[].class);
        ArgumentCaptor<String> fileNameCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> contentTypeCaptor = ArgumentCaptor.forClass(String.class);
        verify(documentStoragePort).store(pathCaptor.capture(), contentCaptor.capture(), fileNameCaptor.capture(), contentTypeCaptor.capture());
        assertEquals("report.pdf", fileNameCaptor.getValue());
        assertEquals("application/pdf", contentTypeCaptor.getValue());
        assertEquals("demo-pdf", new String(contentCaptor.getValue(), StandardCharsets.UTF_8));
        org.junit.jupiter.api.Assertions.assertTrue(pathCaptor.getValue().startsWith("/项目文档/2026/电信/演示项目/uploads/"));

        ArgumentCaptor<DocDocumentRecordBo> recordCaptor = ArgumentCaptor.forClass(DocDocumentRecordBo.class);
        verify(documentApplicationService).upload(recordCaptor.capture());
        DocDocumentRecordBo recordBo = recordCaptor.getValue();
        assertEquals(9L, recordBo.getProjectId());
        assertEquals(DocDocumentSourceType.UPLOAD.getCode(), recordBo.getSourceType());
        assertEquals("stored-report.pdf", recordBo.getFileName());
        assertEquals("/stored/report.pdf", recordBo.getNasPath());
        assertEquals(88L, recordBo.getOssId());
    }

    @Test
    void shouldDeleteDocumentById() {
        DocDocumentRecordController controller = new DocDocumentRecordController(
            documentApplicationService, documentQueryApplicationService, documentViewerApplicationService, projectQueryApplicationService, documentStoragePort
        );

        R<Void> result = controller.delete(66L);

        assertEquals(R.SUCCESS, result.getCode());
        verify(documentApplicationService).delete(66L);
    }

    @Test
    void shouldCreateViewerTicketThroughApplicationService() {
        DocDocumentRecordController controller = new DocDocumentRecordController(
            documentApplicationService, documentQueryApplicationService, documentViewerApplicationService, projectQueryApplicationService, documentStoragePort
        );
        DocViewerTicketVo ticketVo = new DocViewerTicketVo();
        ticketVo.setTicket("opaque-ticket");
        ticketVo.setDocumentId(7L);
        when(documentViewerApplicationService.createViewerTicket(7L)).thenReturn(ticketVo);

        R<DocViewerTicketVo> result = controller.createViewerTicket(7L);

        assertEquals(R.SUCCESS, result.getCode());
        assertEquals(ticketVo, result.getData());
        verify(documentViewerApplicationService).createViewerTicket(7L);
    }
}
