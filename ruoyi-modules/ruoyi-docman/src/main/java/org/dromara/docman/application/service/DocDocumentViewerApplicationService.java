package org.dromara.docman.application.service;

import cn.hutool.core.util.StrUtil;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.constant.HttpStatus;
import org.dromara.common.redis.utils.RedisUtils;
import org.dromara.docman.config.DocmanViewerConfig;
import org.dromara.docman.domain.entity.DocDocumentRecord;
import org.dromara.docman.domain.enums.DocProjectAction;
import org.dromara.docman.domain.vo.DocViewerTicketVo;
import org.dromara.docman.domain.vo.DocViewerUrlVo;
import org.dromara.docman.service.IDocDocumentRecordService;
import org.dromara.docman.service.IDocProjectAccessService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Function;

@Service
public class DocDocumentViewerApplicationService {

    static final String VIEWER_TICKET_PREFIX = "docman:viewer:ticket:";
    static final String PREVIEW_MODE = "preview";
    private final BiConsumer<String, ViewerTicketCachePayload> viewerTicketStore;
    private final Function<String, ViewerTicketCachePayload> viewerTicketLoader;

    private final IDocDocumentRecordService documentRecordService;
    private final IDocProjectAccessService projectAccessService;
    private final DocmanViewerConfig viewerConfig;
    private final DocDocumentApplicationService documentApplicationService;

    @Autowired
    public DocDocumentViewerApplicationService(IDocDocumentRecordService documentRecordService,
                                               IDocProjectAccessService projectAccessService,
                                               DocmanViewerConfig viewerConfig,
                                               DocDocumentApplicationService documentApplicationService) {
        this(documentRecordService, projectAccessService, viewerConfig, documentApplicationService,
            (key, ticket) -> RedisUtils.setCacheObject(key, ticket.toRedisPayload(), Duration.ofSeconds(ticket.ttlSeconds())),
            key -> deserializeViewerTicket(RedisUtils.getCacheObject(key)));
    }

    DocDocumentViewerApplicationService(IDocDocumentRecordService documentRecordService,
                                        IDocProjectAccessService projectAccessService,
                                        DocmanViewerConfig viewerConfig,
                                        DocDocumentApplicationService documentApplicationService,
                                        BiConsumer<String, ViewerTicketCachePayload> viewerTicketStore) {
        this(documentRecordService, projectAccessService, viewerConfig, documentApplicationService, viewerTicketStore,
            key -> deserializeViewerTicket(RedisUtils.getCacheObject(key)));
    }

    DocDocumentViewerApplicationService(IDocDocumentRecordService documentRecordService,
                                        IDocProjectAccessService projectAccessService,
                                        DocmanViewerConfig viewerConfig,
                                        DocDocumentApplicationService documentApplicationService,
                                        BiConsumer<String, ViewerTicketCachePayload> viewerTicketStore,
                                        Function<String, ViewerTicketCachePayload> viewerTicketLoader) {
        this.documentRecordService = documentRecordService;
        this.projectAccessService = projectAccessService;
        this.viewerConfig = viewerConfig;
        this.documentApplicationService = documentApplicationService;
        this.viewerTicketStore = viewerTicketStore;
        this.viewerTicketLoader = viewerTicketLoader;
    }

    /**
     * 为文档创建在线预览票据并写入缓存。
     *
     * @param documentId 文档记录ID
     * @return 预览票据
     */
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
        ticketVo.setUserId(resolveCurrentUserId());
        ticketVo.setMode(PREVIEW_MODE);
        ticketVo.setSaveUrl(null);
        ticketVo.setSaveToken(null);
        ticketVo.setExpireAt(expireAt);

        viewerTicketStore.accept(buildTicketKey(ticket), ViewerTicketCachePayload.from(ticketVo, ticketTtlSeconds));
        return ticketVo;
    }

    /**
     * 生成在线预览URL。
     *
     * @param documentId 文档记录ID
     * @return 预览地址VO
     */
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
        String encodedSrc = UriUtils.encode(src, StandardCharsets.UTF_8);
        String viewerUrl = UriComponentsBuilder.fromUriString(viewerConfig.getBaseUrl())
            .query("src=" + encodedSrc)
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

    /**
     * 基于票据读取在线预览内容。
     *
     * @param ticket 预览票据
     * @return 预览内容载荷
     */
    public ViewerContentPayload loadViewerContent(String ticket) {
        ensureViewerEnabled();
        ViewerTicketCachePayload storedTicket = viewerTicketLoader.apply(buildTicketKey(ticket));
        if (storedTicket == null) {
            throw invalidViewerTicket();
        }
        DocViewerTicketVo payload = storedTicket.toTicketVo();
        if (payload.getExpireAt() == null || Instant.now().isAfter(payload.getExpireAt())) {
            throw invalidViewerTicket();
        }

        DocDocumentRecord record = documentRecordService.queryEntityById(payload.getDocumentId());
        if (!Objects.equals(payload.getProjectId(), record.getProjectId())) {
            throw invalidViewerTicket();
        }
        byte[] content = documentApplicationService.loadDocumentContent(record);
        return new ViewerContentPayload(
            documentApplicationService.resolveFileName(record),
            documentApplicationService.resolveContentType(record),
            content
        );
    }

    /**
     * 校验在线预览能力是否启用。
     */
    public void ensureViewerEnabled() {
        if (!viewerConfig.isEnabled()) {
            throw new ServiceException("文档在线预览未启用", HttpStatus.NOT_IMPLEMENTED);
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
            throw invalidViewerTicket();
        }
        return VIEWER_TICKET_PREFIX + ticket;
    }

    private ServiceException invalidViewerTicket() {
        return new ServiceException("文档预览票据无效或已过期", HttpStatus.NOT_FOUND);
    }

    static ViewerTicketCachePayload deserializeViewerTicket(Object rawPayload) {
        if (rawPayload == null) {
            return null;
        }
        if (rawPayload instanceof ViewerTicketCachePayload payload) {
            return payload;
        }
        if (rawPayload instanceof Map<?, ?> map) {
            return ViewerTicketCachePayload.fromMap(map);
        }
        return null;
    }

    record ViewerTicketCachePayload(Long documentId, Long projectId, Long userId, String mode,
                                    Long expireAtEpochSecond, Long ttlSeconds) {

        static ViewerTicketCachePayload from(DocViewerTicketVo ticketVo, long ttlSeconds) {
            return new ViewerTicketCachePayload(
                ticketVo.getDocumentId(),
                ticketVo.getProjectId(),
                ticketVo.getUserId(),
                ticketVo.getMode(),
                ticketVo.getExpireAt() == null ? null : ticketVo.getExpireAt().getEpochSecond(),
                ttlSeconds
            );
        }

        Map<String, Object> toRedisPayload() {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("documentId", documentId);
            payload.put("projectId", projectId);
            payload.put("userId", userId);
            payload.put("mode", mode);
            payload.put("expireAtEpochSecond", expireAtEpochSecond);
            payload.put("ttlSeconds", ttlSeconds);
            return payload;
        }

        static ViewerTicketCachePayload fromMap(Map<?, ?> map) {
            return new ViewerTicketCachePayload(
                asLong(map.get("documentId")),
                asLong(map.get("projectId")),
                asLong(map.get("userId")),
                map.get("mode") == null ? null : String.valueOf(map.get("mode")),
                asLong(map.get("expireAtEpochSecond")),
                asLong(map.get("ttlSeconds"))
            );
        }

        DocViewerTicketVo toTicketVo() {
            DocViewerTicketVo ticketVo = new DocViewerTicketVo();
            ticketVo.setDocumentId(documentId);
            ticketVo.setProjectId(projectId);
            ticketVo.setUserId(userId);
            ticketVo.setMode(mode);
            ticketVo.setExpireAt(expireAtEpochSecond == null ? null : Instant.ofEpochSecond(expireAtEpochSecond));
            return ticketVo;
        }

        private static Long asLong(Object value) {
            if (value == null) {
                return null;
            }
            if (value instanceof Number number) {
                return number.longValue();
            }
            return Long.parseLong(String.valueOf(value));
        }
    }

    public record ViewerContentPayload(String fileName, String contentType, byte[] content) {
    }

    private Long resolveCurrentUserId() {
        try {
            Class<?> loginHelperClass = Class.forName("org.dromara.common.satoken.utils.LoginHelper");
            Object userId = loginHelperClass.getMethod("getUserId").invoke(null);
            if (userId instanceof Long longValue) {
                return longValue;
            }
            if (userId instanceof Number number) {
                return number.longValue();
            }
            return userId == null ? null : Long.parseLong(String.valueOf(userId));
        } catch (ReflectiveOperationException e) {
            throw new ServiceException("无法获取当前登录用户");
        }
    }
}
