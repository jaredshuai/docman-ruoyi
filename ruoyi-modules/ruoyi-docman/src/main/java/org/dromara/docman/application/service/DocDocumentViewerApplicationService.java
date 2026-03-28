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
import org.dromara.docman.service.IDocDocumentRecordService;
import org.dromara.docman.service.IDocProjectAccessService;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.function.BiConsumer;

@Service
@RequiredArgsConstructor
public class DocDocumentViewerApplicationService {

    static final String VIEWER_TICKET_PREFIX = "docman:viewer:ticket:";
    static final String PREVIEW_MODE = "preview";
    private final BiConsumer<String, StoredViewerTicket> viewerTicketStore;

    private final IDocDocumentRecordService documentRecordService;
    private final IDocProjectAccessService projectAccessService;
    private final DocmanViewerConfig viewerConfig;

    public DocDocumentViewerApplicationService(IDocDocumentRecordService documentRecordService,
                                               IDocProjectAccessService projectAccessService,
                                               DocmanViewerConfig viewerConfig) {
        this(documentRecordService, projectAccessService, viewerConfig,
            (key, ticket) -> RedisUtils.setCacheObject(key, ticket, Duration.ofSeconds(ticket.ttlSeconds())));
    }

    DocDocumentViewerApplicationService(IDocDocumentRecordService documentRecordService,
                                        IDocProjectAccessService projectAccessService,
                                        DocmanViewerConfig viewerConfig,
                                        BiConsumer<String, StoredViewerTicket> viewerTicketStore) {
        this.documentRecordService = documentRecordService;
        this.projectAccessService = projectAccessService;
        this.viewerConfig = viewerConfig;
        this.viewerTicketStore = viewerTicketStore;
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
        ticketVo.setExpireAt(expireAt);

        viewerTicketStore.accept(buildTicketKey(ticket), new StoredViewerTicket(ticketVo, ticketTtlSeconds));
        return ticketVo;
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
}
