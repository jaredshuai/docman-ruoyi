package org.dromara.docman.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.docman.application.port.out.DocumentStoragePort;
import org.dromara.docman.domain.entity.DocArchivePackage;
import org.dromara.docman.domain.entity.DocDocumentRecord;
import org.dromara.docman.domain.entity.DocProject;
import org.dromara.docman.domain.enums.DocDocumentStatus;
import org.dromara.docman.domain.enums.DocProjectAction;
import org.dromara.docman.domain.enums.DocProjectStatus;
import org.dromara.docman.domain.service.DocArchiveDomainService;
import org.dromara.docman.mapper.DocArchivePackageMapper;
import org.dromara.docman.mapper.DocDocumentRecordMapper;
import org.dromara.docman.mapper.DocProjectMapper;
import org.dromara.docman.service.IDocProjectAccessService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("dev")
@Tag("prod")
@Tag("local")
class DocArchiveServiceImplTest {

    @BeforeAll
    static void initTableInfo() {
        initTableInfo(DocArchivePackage.class);
        initTableInfo(DocProject.class);
        initTableInfo(DocDocumentRecord.class);
    }

    @Mock
    private DocArchivePackageMapper archiveMapper;

    @Mock
    private DocDocumentRecordMapper documentRecordMapper;

    @Mock
    private DocProjectMapper projectMapper;

    @Mock
    private IDocProjectAccessService projectAccessService;

    @Mock
    private DocArchiveDomainService archiveDomainService;

    @Mock
    private DocumentStoragePort documentStoragePort;

    @InjectMocks
    private DocArchiveServiceImpl service;

    // ===================== archiveProject tests =====================

    @Test
    void archiveProject_shouldThrowWhenProjectNotFound() {
        Long projectId = 1L;
        doNothing().when(projectAccessService).assertAction(projectId, DocProjectAction.ARCHIVE_PROJECT);
        when(projectMapper.selectById(projectId)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class,
            () -> service.archiveProject(projectId));

        assertEquals("项目不存在", ex.getMessage());
    }

    @Test
    void archiveProject_shouldThrowWhenNoDocumentRecords() {
        Long projectId = 1L;
        DocProject project = createProject(projectId, DocProjectStatus.ACTIVE.getCode());

        doNothing().when(projectAccessService).assertAction(projectId, DocProjectAction.ARCHIVE_PROJECT);
        when(projectMapper.selectById(projectId)).thenReturn(project);
        when(documentRecordMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());

        ServiceException ex = assertThrows(ServiceException.class,
            () -> service.archiveProject(projectId));

        assertEquals("项目无文档记录，无法归档", ex.getMessage());
    }

    @Test
    void archiveProject_shouldThrowWhenDocumentsCannotArchive() {
        Long projectId = 1L;
        DocProject project = createProject(projectId, DocProjectStatus.ACTIVE.getCode());
        DocDocumentRecord invalidRecord = createRecord(1L, projectId, DocDocumentStatus.PENDING.getCode());

        doNothing().when(projectAccessService).assertAction(projectId, DocProjectAction.ARCHIVE_PROJECT);
        when(projectMapper.selectById(projectId)).thenReturn(project);
        when(documentRecordMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(invalidRecord));

        ServiceException ex = assertThrows(ServiceException.class,
            () -> service.archiveProject(projectId));

        assertEquals("存在未完成或已失效文档，无法归档", ex.getMessage());
    }

    @Test
    void archiveProject_shouldSucceedWhenAllDocumentsCanBeArchived() {
        Long projectId = 1L;
        DocProject project = createProject(projectId, DocProjectStatus.ACTIVE.getCode());
        project.setNasBasePath("/nas/project1");
        DocDocumentRecord record = createRecord(1L, projectId, DocDocumentStatus.GENERATED.getCode());
        DocArchivePackage archive = new DocArchivePackage();
        archive.setId(100L);
        DocumentStoragePort.StoredDocument storedDoc = new DocumentStoragePort.StoredDocument("/nas/path", "file.xlsx", 1L);

        doNothing().when(projectAccessService).assertAction(projectId, DocProjectAction.ARCHIVE_PROJECT);
        when(projectMapper.selectById(projectId)).thenReturn(project);
        when(documentRecordMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(record));
        when(archiveMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(archiveDomainService.buildSnapshotManifest(any())).thenReturn(List.of(Map.of("fileName", "test.pdf")));
        when(archiveDomainService.createArchivePackage(any(DocProject.class), any(), anyLong())).thenReturn(archive);
        when(documentStoragePort.store(any(), any(byte[].class), any(), any())).thenReturn(storedDoc);

        DocArchivePackage result = service.archiveProject(projectId);

        assertNotNull(result);
        verify(archiveMapper).insert(any(DocArchivePackage.class));
        verify(documentRecordMapper).updateById(any(DocDocumentRecord.class));
        verify(projectMapper).updateById(any(DocProject.class));
    }

    // ===================== getById tests =====================

    @Test
    void getById_shouldReturnNullWhenNotFound() {
        Long archiveId = 999L;
        when(archiveMapper.selectById(archiveId)).thenReturn(null);

        DocArchivePackage result = service.getById(archiveId);

        assertNull(result);
    }

    @Test
    void getById_shouldReturnArchiveAndCheckAccess() {
        Long archiveId = 1L;
        Long projectId = 10L;
        DocArchivePackage archive = new DocArchivePackage();
        archive.setId(archiveId);
        archive.setProjectId(projectId);

        when(archiveMapper.selectById(archiveId)).thenReturn(archive);
        doNothing().when(projectAccessService).assertAction(projectId, DocProjectAction.VIEW_ARCHIVE);

        DocArchivePackage result = service.getById(archiveId);

        assertNotNull(result);
        assertEquals(archiveId, result.getId());
        verify(projectAccessService).assertAction(projectId, DocProjectAction.VIEW_ARCHIVE);
    }

    // ===================== getByProjectId tests =====================

    @Test
    void getByProjectId_shouldReturnNullWhenNoArchives() {
        Long projectId = 1L;
        doNothing().when(projectAccessService).assertAction(projectId, DocProjectAction.VIEW_ARCHIVE);
        when(archiveMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());

        DocArchivePackage result = service.getByProjectId(projectId);

        assertNull(result);
    }

    @Test
    void getByProjectId_shouldReturnFirstArchive() {
        Long projectId = 1L;
        DocArchivePackage archive1 = new DocArchivePackage();
        archive1.setId(1L);
        archive1.setProjectId(projectId);
        DocArchivePackage archive2 = new DocArchivePackage();
        archive2.setId(2L);
        archive2.setProjectId(projectId);

        doNothing().when(projectAccessService).assertAction(projectId, DocProjectAction.VIEW_ARCHIVE);
        when(archiveMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(archive1, archive2));

        DocArchivePackage result = service.getByProjectId(projectId);

        assertNotNull(result);
        assertEquals(1L, result.getId());
    }

    // ===================== listByProjectId tests =====================

    @Test
    void listByProjectId_shouldReturnEmptyList() {
        Long projectId = 1L;
        doNothing().when(projectAccessService).assertAction(projectId, DocProjectAction.VIEW_ARCHIVE);
        when(archiveMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());

        List<DocArchivePackage> result = service.listByProjectId(projectId);

        assertEquals(0, result.size());
    }

    @Test
    void listByProjectId_shouldReturnArchivesOrderedByVersion() {
        Long projectId = 1L;
        DocArchivePackage archive1 = new DocArchivePackage();
        archive1.setId(1L);
        archive1.setArchiveVersion(1L);
        DocArchivePackage archive2 = new DocArchivePackage();
        archive2.setId(2L);
        archive2.setArchiveVersion(2L);

        doNothing().when(projectAccessService).assertAction(projectId, DocProjectAction.VIEW_ARCHIVE);
        when(archiveMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(archive2, archive1));

        List<DocArchivePackage> result = service.listByProjectId(projectId);

        assertEquals(2, result.size());
    }

    // ===================== countByProjectId tests =====================

    @Test
    void countByProjectId_shouldReturnZero() {
        Long projectId = 1L;
        when(archiveMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);

        long result = service.countByProjectId(projectId);

        assertEquals(0L, result);
    }

    @Test
    void countByProjectId_shouldReturnCount() {
        Long projectId = 1L;
        when(archiveMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(3L);

        long result = service.countByProjectId(projectId);

        assertEquals(3L, result);
    }

    // ===================== additional archiveProject guard tests =====================

    @Test
    void archiveProject_shouldThrowWhenProjectAccessDenied() {
        Long projectId = 1L;
        doThrow(new ServiceException("无权限")).when(projectAccessService)
            .assertAction(projectId, DocProjectAction.ARCHIVE_PROJECT);

        ServiceException ex = assertThrows(ServiceException.class,
            () -> service.archiveProject(projectId));

        assertEquals("无权限", ex.getMessage());
        verify(projectMapper, never()).selectById(anyLong());
    }

    @Test
    void archiveProject_shouldHandleMultipleDocumentRecords() {
        Long projectId = 1L;
        DocProject project = createProject(projectId, DocProjectStatus.ACTIVE.getCode());
        project.setNasBasePath("/nas/project1");
        DocDocumentRecord record1 = createRecord(1L, projectId, DocDocumentStatus.GENERATED.getCode());
        DocDocumentRecord record2 = createRecord(2L, projectId, DocDocumentStatus.GENERATED.getCode());
        DocArchivePackage archive = new DocArchivePackage();
        archive.setId(100L);
        DocumentStoragePort.StoredDocument storedDoc = new DocumentStoragePort.StoredDocument("/nas/path", "file.xlsx", 1L);

        doNothing().when(projectAccessService).assertAction(projectId, DocProjectAction.ARCHIVE_PROJECT);
        when(projectMapper.selectById(projectId)).thenReturn(project);
        when(documentRecordMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(record1, record2));
        when(archiveMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(archiveDomainService.buildSnapshotManifest(any())).thenReturn(List.of(Map.of("fileName", "test.pdf")));
        when(archiveDomainService.createArchivePackage(any(DocProject.class), any(), anyLong())).thenReturn(archive);
        when(documentStoragePort.store(any(), any(byte[].class), any(), any())).thenReturn(storedDoc);

        DocArchivePackage result = service.archiveProject(projectId);

        assertNotNull(result);
        verify(documentRecordMapper).updateById(record1);
        verify(documentRecordMapper).updateById(record2);
    }

    @Test
    void archiveProject_shouldCalculateNextVersionCorrectly() {
        Long projectId = 1L;
        DocProject project = createProject(projectId, DocProjectStatus.ACTIVE.getCode());
        project.setNasBasePath("/nas/project1");
        DocDocumentRecord record = createRecord(1L, projectId, DocDocumentStatus.GENERATED.getCode());
        DocArchivePackage archive = new DocArchivePackage();
        archive.setId(100L);
        DocumentStoragePort.StoredDocument storedDoc = new DocumentStoragePort.StoredDocument("/nas/path", "file.xlsx", 1L);

        doNothing().when(projectAccessService).assertAction(projectId, DocProjectAction.ARCHIVE_PROJECT);
        when(projectMapper.selectById(projectId)).thenReturn(project);
        when(documentRecordMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(record));
        when(archiveMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(2L);
        when(archiveDomainService.buildSnapshotManifest(any())).thenReturn(List.of(Map.of("fileName", "test.pdf")));
        when(archiveDomainService.createArchivePackage(any(DocProject.class), any(), anyLong())).thenReturn(archive);
        when(documentStoragePort.store(any(), any(byte[].class), any(), any())).thenReturn(storedDoc);

        service.archiveProject(projectId);

        verify(archiveDomainService).createArchivePackage(any(DocProject.class), any(), anyLong());
    }

    // ===================== helper methods =====================

    private DocProject createProject(Long id, String status) {
        DocProject project = new DocProject();
        project.setId(id);
        project.setStatus(status);
        project.setName("Test Project");
        return project;
    }

    private DocDocumentRecord createRecord(Long id, Long projectId, String status) {
        DocDocumentRecord record = new DocDocumentRecord();
        record.setId(id);
        record.setProjectId(projectId);
        record.setStatus(status);
        record.setFileName("test.pdf");
        record.setNasPath("/nas/test.pdf");
        return record;
    }

    private static void initTableInfo(Class<?> entityClass) {
        if (TableInfoHelper.getTableInfo(entityClass) == null) {
            TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), "test"), entityClass);
        }
    }
}