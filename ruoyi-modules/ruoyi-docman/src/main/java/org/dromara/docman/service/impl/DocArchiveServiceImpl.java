package org.dromara.docman.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.docman.domain.entity.DocArchivePackage;
import org.dromara.docman.domain.entity.DocDocumentRecord;
import org.dromara.docman.domain.entity.DocProject;
import org.dromara.docman.domain.enums.DocArchiveStatus;
import org.dromara.docman.domain.enums.DocDocumentStatus;
import org.dromara.docman.domain.enums.DocProjectAction;
import org.dromara.docman.domain.service.DocArchiveDomainService;
import org.dromara.docman.domain.service.DocDocumentStateMachine;
import org.dromara.docman.mapper.DocArchivePackageMapper;
import org.dromara.docman.mapper.DocDocumentRecordMapper;
import org.dromara.docman.mapper.DocProjectMapper;
import org.dromara.docman.service.IDocArchiveService;
import org.dromara.docman.service.IDocProjectAccessService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocArchiveServiceImpl implements IDocArchiveService {

    private final DocArchivePackageMapper archiveMapper;
    private final DocDocumentRecordMapper documentRecordMapper;
    private final DocProjectMapper projectMapper;
    private final IDocProjectAccessService projectAccessService;
    private final DocArchiveDomainService archiveDomainService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DocArchivePackage archiveProject(Long projectId) {
        projectAccessService.assertAction(projectId, DocProjectAction.ARCHIVE_PROJECT);
        DocProject project = projectMapper.selectById(projectId);
        if (project == null) {
            throw new ServiceException("项目不存在");
        }
        if ("archived".equals(project.getStatus())) {
            throw new ServiceException("项目已归档");
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

        // TODO: 生成归档清单 Excel 并上传到 NAS
        // byte[] manifestExcel = generateManifestExcel(manifest);
        // String manifestPath = project.getNasBasePath() + "/归档清单.xlsx";
        // nasService.uploadFile(manifestPath, manifestExcel, "归档清单.xlsx");

        DocArchivePackage archive = archiveDomainService.createArchivePackage(project, records, nextVersion);
        archiveMapper.insert(archive);

        for (DocDocumentRecord record : records) {
            DocDocumentStateMachine.checkTransition(DocDocumentStatus.of(record.getStatus()), DocDocumentStatus.ARCHIVED);
            record.setStatus(DocDocumentStatus.ARCHIVED.getCode());
            record.setArchivedAt(new Date());
            documentRecordMapper.updateById(record);
        }

        project.setStatus("archived");
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
}
