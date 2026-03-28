package org.dromara.docman.application.service;

import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.redis.utils.RedisUtils;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.docman.config.DocmanViewerConfig;
import org.dromara.docman.domain.entity.DocDocumentRecord;
import org.dromara.docman.domain.enums.DocProjectAction;
import org.dromara.docman.domain.vo.DocViewerTicketVo;
import org.dromara.docman.domain.vo.DocViewerUrlVo;
import org.dromara.docman.service.IDocDocumentRecordService;
import org.dromara.docman.service.IDocProjectAccessService;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
public class DocDocumentViewerApplicationService {

    static final String VIEWER_TICKET_PREFIX = "docman:viewer:ticket:";
    static final String PREVIEW_MODE = "preview";
    private final BiConsumer<String, StoredViewerTicket> viewerTicketStore;
    private final Function<String, StoredViewerTicket> viewerTicketLoader;

    private final IDocDocumentRecordService documentRecordService;
    private final IDocProjectAccessService projectAccessService;
    private final DocmanViewerConfig viewerConfig;
    private final DocDocumentApplicationService documentApplicationService;

    public DocDocumentViewerApplicationService(IDocDocumentRecordService documentRecordService,
                                               IDocProjectAccessService projectAccessService,
                                               DocmanViewerConfig viewerConfig,
                                               DocDocumentApplicationService documentApplicationService) {
        this(documentRecordService, projectAccessService, viewerConfig, documentApplicationService,
            (key, ticket) -> RedisUtils.setCacheObject(key, ticket, Duration.ofSeconds(ticket.ttlSeconds())));
    }

    DocDocumentViewerApplicationService(IDocDocumentRecordService documentRecordService,
                                        IDocProjectAccessService projectAccessService,
                                        DocmanViewerConfig viewerConfig,
                                        DocDocumentApplicationService documentApplicationService,
                                        BiConsumer<String, StoredViewerTicket> viewerTicketStore) {
        this(documentRecordService, projectAccessService, viewerConfig, documentApplicationService,
            viewerTicketStore, key -> RedisUtils.getCacheObject(key));
    }

    DocDocumentViewerApplicationService(IDocDocumentRecordService documentRecordService,
                                        IDocProjectAccessService projectAccessService,
                                        DocmanViewerConfig viewerConfig,
                                        DocDocumentApplicationService documentApplicationService,
                                        BiConsumer<String, StoredViewerTicket> viewerTicketStore,
                                        Function<String, StoredViewerTicket> viewerTicketLoader) {
        this.documentRecordService = documentRecordService;
        this.projectAccessService = projectAccessService;
        this.viewerConfig = viewerConfig;
        this.documentApplicationService = documentApplicationService;
        this.viewerTicketStore = viewerTicketStore;
        this.viewerTicketLoader = viewerTicketLoader;
    }

    public DocViewerTicketVo createViewerTicket(Long documentId) {
        ensureViewerEnabled();
        DocDocumentRecord record = documentRecordService.queryEntityById(documentId);
        projectAccessService.assertAction(record.getProjectId(), DocProjectAction.VIEW_DOCUMENT);

        long ticketTtlSeconds = resolveTicketTtlSeconds();
        String ticket = UUID.randomUUID().toString().replace("-", "");
        Instant expireAt = Instant.now().plusSeconds(ticketTtlSeconds);

        DocViewerTicketVo ticketVo = new DocViewerTicketVo();
        ticketVo.setTicket(ticket);
        ticketVo.setDocumentId(record.getId());
        ticketVo.setProjectId(record.getProjectId());
        ticketVo.setUserId(LoginHelper.getUserId());
        ticketVo.setMode(PREVIEW_MODE);
        ticketVo.setSaveUrl(null);
        ticketVo.setSaveToken(null);
        ticketVo.setExpireAt(expireAt);

        viewerTicketStore.accept(buildTicketKey(ticket), new StoredViewerTicket(ticketVo, ticketTtlSeconds));
        return ticketVo;
    }

    public DocViewerUrlVo getViewerUrl(Long documentId) {
        ensureViewerEnabled();
        if (StrUtil.isBlank(viewerConfig.getBaseUrl())) {
            throw new ServiceException("文档在线预览服务地址未配置");
        }

        DocViewerTicketVo ticketVo = createViewerTicket(documentId);
        String src = ServletUriComponentsBuilder.fromCurrentContextPath()
            .path("/docman/document/viewer/content/{ticket}")
            .buildAndExpand(ticketVo.getTicket())
            .encode()
            .toUriString();
        String viewerUrl = UriComponentsBuilder.fromUriString(viewerConfig.getBaseUrl())
            .queryParam("src", src)
            .queryParam("mode", PREVIEW_MODE)
            .build(true)
            .toUriString();

        DocViewerUrlVo urlVo = new DocViewerUrlVo();
        urlVo.setUrl(viewerUrl);
        urlVo.setSrc(src);
        urlVo.setMode(PREVIEW_MODE);
        urlVo.setSaveUrl(ticketVo.getSaveUrl());
        urlVo.setSaveToken(ticketVo.getSaveToken());
        urlVo.setExpireAt(ticketVo.getExpireAt());
        return urlVo;
    }

    public ViewerContentPayload loadViewerContent(String ticket) {
        ensureViewerEnabled();
        StoredViewerTicket storedTicket = viewerTicketLoader.apply(buildTicketKey(ticket));
        if (storedTicket == null || storedTicket.payload() == null) {
            throw new ServiceException("文档预览票据无效或已过期");
        }

        DocViewerTicketVo payload = storedTicket.payload();
        if (payload.getExpireAt() == null || Instant.now().isAfter(payload.getExpireAt())) {
            throw new ServiceException("文档预览票据无效或已过期");
        }

        DocDocumentRecord record = documentRecordService.queryEntityById(payload.getDocumentId());
        if (!Objects.equals(payload.getProjectId(), record.getProjectId())) {
            throw new ServiceException("文档预览票据无效或已过期");
        }
        projectAccessService.assertAction(record.getProjectId(), DocProjectAction.VIEW_DOCUMENT);
        byte[] content = documentApplicationService.loadDocumentContent(record);
        return new ViewerContentPayload(
            documentApplicationService.resolveFileName(record),
            documentApplicationService.resolveContentType(record),
            content
        );
    }

    public void ensureViewerEnabled() {
        if (!viewerConfig.isEnabled()) {
            throw new ServiceException("文档在线预览未启用");
        }
    }

    private long resolveTicketTtlSeconds() {
        if (viewerConfig.getTicketTtlSeconds() <= 0) {
            throw new ServiceException("文档预览票据TTL配置非法");
        }
        return viewerConfig.getTicketTtlSeconds();
    }

    private String buildTicketKey(String ticket) {
        if (StrUtil.isBlank(ticket)) {
            throw new ServiceException("文档预览票据不能为空");
        }
        return VIEWER_TICKET_PREFIX + ticket;
    }

    record StoredViewerTicket(DocViewerTicketVo payload, long ttlSeconds) {
    }

    public record ViewerContentPayload(String fileName, String contentType, byte[] content) {
    }
}
