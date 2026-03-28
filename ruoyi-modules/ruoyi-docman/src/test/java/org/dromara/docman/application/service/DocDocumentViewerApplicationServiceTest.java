package org.dromara.docman.application.service;

import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.docman.config.DocmanViewerConfig;
import org.dromara.docman.domain.entity.DocDocumentRecord;
import org.dromara.docman.domain.enums.DocProjectAction;
import org.dromara.docman.domain.vo.DocViewerTicketVo;
import org.dromara.docman.domain.vo.DocViewerUrlVo;
import org.dromara.docman.service.IDocDocumentRecordService;
import org.dromara.docman.service.IDocProjectAccessService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("dev")
@Tag("prod")
@Tag("local")
class DocDocumentViewerApplicationServiceTest {

    @Mock
    private IDocDocumentRecordService documentRecordService;

    @Mock
    private IDocProjectAccessService projectAccessService;

    @Mock
    private DocDocumentApplicationService documentApplicationService;

    @Test
    void shouldCreateOpaqueViewerTicketForAuthorizedUser() {
        DocmanViewerConfig config = new DocmanViewerConfig();
        config.setEnabled(true);
        config.setTicketTtlSeconds(300);
        AtomicReference<String> storedKey = new AtomicReference<>();
        AtomicReference<DocDocumentViewerApplicationService.StoredViewerTicket> storedTicket = new AtomicReference<>();
        DocDocumentViewerApplicationService service = new DocDocumentViewerApplicationService(
            documentRecordService, projectAccessService, config, documentApplicationService,
            (key, ticket) -> {
                storedKey.set(key);
                storedTicket.set(ticket);
            }
        );
        DocDocumentRecord record = new DocDocumentRecord();
        record.setId(11L);
        record.setProjectId(22L);
        when(documentRecordService.queryEntityById(11L)).thenReturn(record);
        doNothing().when(projectAccessService).assertAction(22L, DocProjectAction.VIEW_DOCUMENT);

        try (MockedStatic<LoginHelper> loginHelper = mockStatic(LoginHelper.class)) {
            loginHelper.when(LoginHelper::getUserId).thenReturn(33L);

            DocViewerTicketVo result = service.createViewerTicket(11L);

            assertNotNull(result.getTicket());
            assertEquals(32, result.getTicket().length());
            assertTrue(result.getTicket().matches("[0-9a-fA-F]+"));
            assertEquals(11L, result.getDocumentId());
            assertEquals(22L, result.getProjectId());
            assertEquals(33L, result.getUserId());
            assertEquals("preview", result.getMode());
            assertNull(result.getSaveUrl());
            assertNull(result.getSaveToken());
            assertNotNull(result.getExpireAt());
            assertTrue(result.getExpireAt().isAfter(java.time.Instant.now()));
            verify(projectAccessService).assertAction(22L, DocProjectAction.VIEW_DOCUMENT);
            assertEquals("docman:viewer:ticket:" + result.getTicket(), storedKey.get());
            assertNotNull(storedTicket.get());
            assertEquals(300L, storedTicket.get().ttlSeconds());
            assertEquals(result, storedTicket.get().payload());
        }
    }

    @Test
    void shouldRejectViewerTicketCreationWhenViewerDisabled() {
        DocmanViewerConfig config = new DocmanViewerConfig();
        config.setEnabled(false);
        config.setTicketTtlSeconds(300);
        DocDocumentViewerApplicationService service = new DocDocumentViewerApplicationService(
            documentRecordService, projectAccessService, config, documentApplicationService
        );

        ServiceException ex = assertThrows(ServiceException.class, () -> service.createViewerTicket(10L));

        assertEquals("文档在线预览未启用", ex.getMessage());
        verifyNoInteractions(documentRecordService, projectAccessService);
    }

    @Test
    void shouldPropagatePermissionDenialWhenUserCannotViewDocument() {
        DocmanViewerConfig config = new DocmanViewerConfig();
        config.setEnabled(true);
        config.setTicketTtlSeconds(300);
        DocDocumentViewerApplicationService service = new DocDocumentViewerApplicationService(
            documentRecordService, projectAccessService, config, documentApplicationService, (key, ticket) -> {
                throw new AssertionError("viewer ticket should not be stored");
            }
        );
        DocDocumentRecord record = new DocDocumentRecord();
        record.setId(15L);
        record.setProjectId(26L);
        when(documentRecordService.queryEntityById(15L)).thenReturn(record);
        ServiceException denial = new ServiceException("无权访问该项目文档");
        org.mockito.Mockito.doThrow(denial).when(projectAccessService).assertAction(26L, DocProjectAction.VIEW_DOCUMENT);

        try (MockedStatic<LoginHelper> loginHelper = mockStatic(LoginHelper.class)) {
            loginHelper.when(LoginHelper::getUserId).thenReturn(35L);

            ServiceException ex = assertThrows(ServiceException.class, () -> service.createViewerTicket(15L));

            assertEquals("无权访问该项目文档", ex.getMessage());
        }
    }

    @Test
    void shouldBuildViewerUrlWithEncodedSrcAndReservedPreviewFields() {
        DocmanViewerConfig config = new DocmanViewerConfig();
        config.setEnabled(true);
        config.setBaseUrl("https://viewer.example.com/");
        config.setTicketTtlSeconds(300);
        DocDocumentViewerApplicationService service = new DocDocumentViewerApplicationService(
            documentRecordService, projectAccessService, config, documentApplicationService,
            (key, ticket) -> {
            }
        );
        DocDocumentRecord record = new DocDocumentRecord();
        record.setId(101L);
        record.setProjectId(202L);
        when(documentRecordService.queryEntityById(101L)).thenReturn(record);
        doNothing().when(projectAccessService).assertAction(202L, DocProjectAction.VIEW_DOCUMENT);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setScheme("https");
        request.setServerName("backend.example.com");
        request.setServerPort(18081);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        try (MockedStatic<LoginHelper> loginHelper = mockStatic(LoginHelper.class)) {
            loginHelper.when(LoginHelper::getUserId).thenReturn(303L);

            DocViewerUrlVo result = service.getViewerUrl(101L);

            assertEquals("preview", result.getMode());
            assertNull(result.getSaveUrl());
            assertNull(result.getSaveToken());
            assertTrue(result.getSrc().startsWith("https://backend.example.com:18081/docman/document/viewer/content/"));
            assertTrue(result.getUrl().startsWith("https://viewer.example.com/?src="));
            assertEquals(result.getSrc(), UriComponentsBuilder.fromUriString(result.getUrl()).build().getQueryParams().getFirst("src"));
            assertTrue(result.getUrl().contains("&mode=preview"));
            assertNotNull(result.getExpireAt());
        } finally {
            RequestContextHolder.resetRequestAttributes();
        }
    }

    @Test
    void shouldLoadViewerContentMoreThanOnceWithinTtl() {
        DocmanViewerConfig config = new DocmanViewerConfig();
        config.setEnabled(true);
        config.setTicketTtlSeconds(300);
        DocViewerTicketVo payload = new DocViewerTicketVo();
        payload.setTicket("opaque");
        payload.setDocumentId(501L);
        payload.setProjectId(601L);
        payload.setUserId(701L);
        payload.setMode("preview");
        payload.setExpireAt(java.time.Instant.now().plusSeconds(300));
        DocDocumentViewerApplicationService.StoredViewerTicket storedTicket =
            new DocDocumentViewerApplicationService.StoredViewerTicket(payload, 300L);
        DocDocumentViewerApplicationService service = new DocDocumentViewerApplicationService(
            documentRecordService, projectAccessService, config, documentApplicationService,
            (key, ticket) -> {
            },
            key -> storedTicket
        );
        DocDocumentRecord record = new DocDocumentRecord();
        record.setId(501L);
        record.setProjectId(601L);
        record.setFileName("demo.docx");
        when(documentRecordService.queryEntityById(501L)).thenReturn(record);
        when(documentApplicationService.loadDocumentContent(record)).thenReturn("doc-content".getBytes());
        when(documentApplicationService.resolveFileName(record)).thenReturn("demo.docx");
        when(documentApplicationService.resolveContentType(record))
            .thenReturn("application/vnd.openxmlformats-officedocument.wordprocessingml.document");

        DocDocumentViewerApplicationService.ViewerContentPayload first = service.loadViewerContent("opaque");
        DocDocumentViewerApplicationService.ViewerContentPayload second = service.loadViewerContent("opaque");

        assertEquals("demo.docx", first.fileName());
        assertEquals("application/vnd.openxmlformats-officedocument.wordprocessingml.document", first.contentType());
        assertArrayEquals("doc-content".getBytes(), first.content());
        assertArrayEquals(first.content(), second.content());
        verify(projectAccessService, org.mockito.Mockito.times(2)).assertAction(601L, DocProjectAction.VIEW_DOCUMENT);
    }

    @Test
    void shouldRejectExpiredViewerTicketWhenLoadingContent() {
        DocmanViewerConfig config = new DocmanViewerConfig();
        config.setEnabled(true);
        config.setTicketTtlSeconds(300);
        DocViewerTicketVo payload = new DocViewerTicketVo();
        payload.setTicket("expired");
        payload.setDocumentId(801L);
        payload.setProjectId(901L);
        payload.setExpireAt(java.time.Instant.now().minusSeconds(1));
        DocDocumentViewerApplicationService service = new DocDocumentViewerApplicationService(
            documentRecordService, projectAccessService, config, documentApplicationService,
            (key, ticket) -> {
            },
            key -> new DocDocumentViewerApplicationService.StoredViewerTicket(payload, 300L)
        );

        ServiceException ex = assertThrows(ServiceException.class, () -> service.loadViewerContent("expired"));

        assertEquals("文档预览票据无效或已过期", ex.getMessage());
        verifyNoInteractions(documentApplicationService, documentRecordService, projectAccessService);
    }
}
