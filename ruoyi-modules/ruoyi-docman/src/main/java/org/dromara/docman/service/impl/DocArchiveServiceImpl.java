package org.dromara.docman.service.impl;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import cn.idev.excel.EasyExcel;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.docman.application.port.out.DocumentStoragePort;
import org.dromara.docman.domain.entity.DocArchivePackage;
import org.dromara.docman.domain.entity.DocDocumentRecord;
import org.dromara.docman.domain.entity.DocProject;
import org.dromara.docman.domain.enums.DocDocumentSourceType;
import org.dromara.docman.domain.enums.DocArchiveStatus;
import org.dromara.docman.domain.enums.DocDocumentStatus;
import org.dromara.docman.domain.enums.DocProjectAction;
import org.dromara.docman.domain.enums.DocProjectStatus;
import org.dromara.docman.domain.service.DocArchiveDomainService;
import org.dromara.docman.domain.service.DocDocumentStateMachine;
import org.dromara.docman.domain.service.DocProjectStateMachine;
import org.dromara.docman.mapper.DocArchivePackageMapper;
import org.dromara.docman.mapper.DocDocumentRecordMapper;
import org.dromara.docman.mapper.DocProjectMapper;
import org.dromara.docman.service.IDocArchiveService;
import org.dromara.docman.service.IDocProjectAccessService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocArchiveServiceImpl implements IDocArchiveService {

    private final DocArchivePackageMapper archiveMapper;
    private final DocDocumentRecordMapper documentRecordMapper;
    private final DocProjectMapper projectMapper;
    private final IDocProjectAccessService projectAccessService;
    private final DocArchiveDomainService archiveDomainService;
    private final DocumentStoragePort documentStoragePort;

    private static final String MANIFEST_FILE_PREFIX = "归档清单_V";
    private static final String MANIFEST_CONTENT_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DocArchivePackage archiveProject(Long projectId) {
        projectAccessService.assertAction(projectId, DocProjectAction.ARCHIVE_PROJECT);
        DocProject project = projectMapper.selectById(projectId);
        if (project == null) {
            throw new ServiceException("项目不存在");
        }

        List<DocDocumentRecord> records = documentRecordMapper.selectList(
            new LambdaQueryWrapper<DocDocumentRecord>()
                .eq(DocDocumentRecord::getProjectId, projectId)
                .orderByAsc(DocDocumentRecord::getNasPath)
        );
        if (records.isEmpty()) {
            throw new ServiceException("项目无文档记录，无法归档");
        }

        long invalidCount = records.stream()
            .filter(r -> !DocDocumentStateMachine.canArchive(DocDocumentStatus.of(r.getStatus())))
            .count();
        if (invalidCount > 0) {
            throw new ServiceException("存在未完成或已失效文档，无法归档");
        }

        long nextVersion = countByProjectId(projectId) + 1;
        List<Map<String, String>> manifest = archiveDomainService.buildSnapshotManifest(records);
        String manifestFileName = MANIFEST_FILE_PREFIX + nextVersion + ".xlsx";
        String manifestPath = project.getNasBasePath() + "/" + manifestFileName;

        byte[] manifestExcel = generateManifestExcel(manifest);
        documentStoragePort.store(manifestPath, manifestExcel, manifestFileName, MANIFEST_CONTENT_TYPE);

        List<Map<String, String>> archiveManifest = new ArrayList<>(manifest);
        archiveManifest.add(buildManifestFileEntry(manifestFileName, manifestPath));

        DocArchivePackage archive = archiveDomainService.createArchivePackage(project, archiveManifest, nextVersion);
        archiveMapper.insert(archive);

        for (DocDocumentRecord record : records) {
            DocDocumentStateMachine.checkTransition(DocDocumentStatus.of(record.getStatus()), DocDocumentStatus.ARCHIVED);
            record.setStatus(DocDocumentStatus.ARCHIVED.getCode());
            record.setArchivedAt(new Date());
            documentRecordMapper.updateById(record);
        }

        DocProjectStateMachine.checkTransition(DocProjectStatus.of(project.getStatus()), DocProjectStatus.ARCHIVED);
        project.setStatus(DocProjectStatus.ARCHIVED.getCode());
        projectMapper.updateById(project);

        log.info("项目归档完成: {} ({}份文档)", project.getName(), records.size());
        return archive;
    }

    @Override
    public DocArchivePackage getByProjectId(Long projectId) {
        projectAccessService.assertAction(projectId, DocProjectAction.VIEW_ARCHIVE);
        return listByProjectId(projectId).stream()
            .findFirst()
            .orElse(null);
    }

    @Override
    public List<DocArchivePackage> listByProjectId(Long projectId) {
        projectAccessService.assertAction(projectId, DocProjectAction.VIEW_ARCHIVE);
        return archiveMapper.selectList(
            new LambdaQueryWrapper<DocArchivePackage>()
                .eq(DocArchivePackage::getProjectId, projectId)
                .orderByDesc(DocArchivePackage::getArchiveVersion)
        );
    }

    @Override
    public long countByProjectId(Long projectId) {
        return archiveMapper.selectCount(
            new LambdaQueryWrapper<DocArchivePackage>()
                .eq(DocArchivePackage::getProjectId, projectId)
                .in(DocArchivePackage::getStatus,
                    DocArchiveStatus.REQUESTED.getCode(),
                    DocArchiveStatus.GENERATING.getCode(),
                    DocArchiveStatus.COMPLETED.getCode(),
                    DocArchiveStatus.FAILED.getCode())
        );
    }

    private byte[] generateManifestExcel(List<Map<String, String>> manifest) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            List<List<String>> head = List.of(
                List.of("文件名"),
                List.of("路径"),
                List.of("来源"),
                List.of("生成时间")
            );
            List<List<String>> rows = manifest.stream()
                .map(item -> List.of(
                    getManifestValue(item, "fileName"),
                    getManifestValue(item, "nasPath"),
                    getManifestValue(item, "sourceType"),
                    getManifestValue(item, "generatedAt")
                ))
                .toList();
            EasyExcel.write(outputStream)
                .head(head)
                .autoCloseStream(false)
                .sheet("归档清单")
                .doWrite(rows);
            return outputStream.toByteArray();
        } catch (Exception e) {
            throw new ServiceException("生成归档清单 Excel 失败", e);
        }
    }

    private Map<String, String> buildManifestFileEntry(String manifestFileName, String manifestPath) {
        Map<String, String> manifestEntry = new LinkedHashMap<>();
        manifestEntry.put("fileName", manifestFileName);
        manifestEntry.put("nasPath", manifestPath);
        manifestEntry.put("sourceType", DocDocumentSourceType.ARCHIVE_MANIFEST.getCode());
        manifestEntry.put("status", DocDocumentStatus.GENERATED.getCode());
        manifestEntry.put("generatedAt", DateUtil.format(new Date(), DatePattern.NORM_DATETIME_PATTERN));
        return manifestEntry;
    }

    private String getManifestValue(Map<String, String> item, String key) {
        String value = item.get(key);
        return value == null ? "" : value;
    }
}
