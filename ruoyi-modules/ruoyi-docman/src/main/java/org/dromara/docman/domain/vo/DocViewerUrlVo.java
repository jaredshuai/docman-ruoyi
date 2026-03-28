package org.dromara.docman.domain.vo;

import lombok.Data;

import java.time.Instant;

@Data
public class DocViewerUrlVo {

    private String url;

    private String src;

    private String mode;

    private String saveUrl;

    private String saveToken;

    private Instant expireAt;
}
