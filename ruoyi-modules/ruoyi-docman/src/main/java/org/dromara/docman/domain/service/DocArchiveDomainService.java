package org.dromara.docman.domain.service;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import cn.hutool.crypto.digest.DigestUtil;
import org.dromara.common.json.utils.JsonUtils;
import org.dromara.docman.domain.entity.DocArchivePackage;
import org.dromara.docman.domain.entity.DocDocumentRecord;
import org.dromara.docman.domain.entity.DocProject;
import org.dromara.docman.domain.enums.DocArchiveStatus;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class DocArchiveDomainService {

    public DocArchivePackage createArchivePackage(DocProject project, List<Map<String, String>> snapshotManifest, long nextVersion) {
        Date now = new Date();

        DocArchivePackage archive = new DocArchivePackage();
        archive.setProjectId(project.getId());
        archive.setArchiveNo(buildArchiveNo(project.getId(), nextVersion, now));
        archive.setArchiveVersion(nextVersion);
        archive.setNasArchivePath(project.getNasBasePath());
        archive.setManifest(snapshotManifest);
        archive.setSnapshotChecksum(DigestUtil.sha256Hex(JsonUtils.toJsonString(snapshotManifest)));
        archive.setStatus(DocArchiveStatus.COMPLETED.getCode());
        archive.setRequestedAt(now);
        archive.setCompletedAt(now);
        return archive;
    }

    public List<Map<String, String>> buildSnapshotManifest(List<DocDocumentRecord> records) {
        List<Map<String, String>> manifest = new ArrayList<>();
        for (DocDocumentRecord record : records) {
            Map<String, String> entry = new LinkedHashMap<>();
            entry.put("fileName", record.getFileName());
            entry.put("nasPath", record.getNasPath());
            entry.put("sourceType", record.getSourceType());
            entry.put("status", record.getStatus());
            entry.put("generatedAt", record.getGeneratedAt() != null
                ? DateUtil.format(record.getGeneratedAt(), DatePattern.NORM_DATETIME_PATTERN)
                : "");
            manifest.add(entry);
        }
        return manifest;
    }

    private String buildArchiveNo(Long projectId, long version, Date now) {
        return "ARC-" + DateUtil.format(now, DatePattern.PURE_DATE_PATTERN) + "-" + projectId + "-V" + version;
    }
}
