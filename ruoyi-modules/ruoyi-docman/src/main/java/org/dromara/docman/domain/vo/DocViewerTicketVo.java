package org.dromara.docman.domain.vo;

import lombok.Data;

import java.time.Instant;

@Data
public class DocViewerTicketVo {

    private String ticket;

    private Long documentId;

    private Long projectId;

    private Long userId;

    private String mode;

    private Instant expireAt;
}
