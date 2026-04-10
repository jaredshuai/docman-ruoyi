package org.dromara.docman.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.docman.application.assembler.DocDocumentAssembler;
import org.dromara.docman.domain.bo.DocDocumentRecordBo;
import org.dromara.docman.domain.entity.DocDocumentRecord;
import org.dromara.docman.domain.enums.DocDocumentSourceType;
import org.dromara.docman.domain.enums.DocDocumentStatus;
import org.dromara.docman.domain.enums.DocProjectAction;
import org.dromara.docman.domain.vo.DocDocumentRecordVo;
import org.dromara.docman.mapper.DocDocumentRecordMapper;
import org.dromara.docman.plugin.PluginResult;
import org.dromara.docman.service.IDocProjectAccessService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("dev")
@Tag("prod")
@Tag("local")
class DocDocumentRecordServiceImplTest {

    @BeforeAll
    static void initTableInfo() {
        initTableInfo(DocDocumentRecord.class);
    }

    @Mock
    private DocDocumentRecordMapper baseMapper;

    @Mock
    private IDocProjectAccessService projectAccessService;

    @Mock
    private DocDocumentAssembler documentAssembler;

    @InjectMocks
    private DocDocumentRecordServiceImpl service;

    // ==================== queryPageList ====================

    @Test
    void queryPageList_shouldDelegatePermissionCheck() {
        Long projectId = 100L;
        PageQuery pageQuery = new PageQuery(10, 1);
        Page<DocDocumentRecordVo> mockPage = new Page<>(1, 10);
        mockPage.setRecords(List.of());
        mockPage.setTotal(0);

        doNothing().when(projectAccessService).assertAction(projectId, DocProjectAction.VIEW_DOCUMENT);
        when(baseMapper.selectVoPage(any(), any(LambdaQueryWrapper.class))).thenReturn(mockPage);

        TableDataInfo<DocDocumentRecordVo> result = service.queryPageList(projectId, pageQuery);

        verify(projectAccessService).assertAction(projectId, DocProjectAction.VIEW_DOCUMENT);
        assertNotNull(result);
        assertEquals(0, result.getTotal());
    }

    // ==================== queryById ====================

    @Test
    void queryById_shouldThrowWhenRecordNotFound() {
        Long id = 999L;
        when(baseMapper.selectById(id)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class, () -> service.queryById(id));

        assertEquals("文档记录不存在", ex.getMessage());
        verify(baseMapper, never()).selectVoById(any());
    }

    @Test
    void queryById_shouldReturnVoWhenRecordExists() {
        Long id = 1L;
        Long projectId = 100L;
        DocDocumentRecord record = createRecord(id, projectId, DocDocumentStatus.GENERATED.getCode());
        DocDocumentRecordVo vo = createVo(id, projectId);

        when(baseMapper.selectById(id)).thenReturn(record);
        doNothing().when(projectAccessService).assertAction(projectId, DocProjectAction.VIEW_DOCUMENT);
        when(baseMapper.selectVoById(id)).thenReturn(vo);

        DocDocumentRecordVo result = service.queryById(id);

        assertNotNull(result);
        assertEquals(id, result.getId());
        assertEquals(projectId, result.getProjectId());
        verify(projectAccessService).assertAction(projectId, DocProjectAction.VIEW_DOCUMENT);
    }

    // ==================== queryEntityById ====================

    @Test
    void queryEntityById_shouldThrowWhenRecordNotFound() {
        Long id = 999L;
        when(baseMapper.selectById(id)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class, () -> service.queryEntityById(id));

        assertEquals("文档记录不存在", ex.getMessage());
    }

    @Test
    void queryEntityById_shouldReturnEntityWhenRecordExists() {
        Long id = 1L;
        Long projectId = 100L;
        DocDocumentRecord record = createRecord(id, projectId, DocDocumentStatus.GENERATED.getCode());

        when(baseMapper.selectById(id)).thenReturn(record);
        doNothing().when(projectAccessService).assertAction(projectId, DocProjectAction.VIEW_DOCUMENT);

        DocDocumentRecord result = service.queryEntityById(id);

        assertNotNull(result);
        assertEquals(id, result.getId());
        assertEquals(projectId, result.getProjectId());
        verify(projectAccessService).assertAction(projectId, DocProjectAction.VIEW_DOCUMENT);
    }

    // ==================== recordUpload ====================

    @Test
    void recordUpload_shouldCreateRecordWithUploadSourceType() {
        Long projectId = 100L;
        DocDocumentRecordBo bo = new DocDocumentRecordBo();
        bo.setProjectId(projectId);
        bo.setFileName("test.pdf");
        bo.setNasPath("/docs/test.pdf");
        bo.setOssId(12345L);

        DocDocumentRecord record = new DocDocumentRecord();
        record.setProjectId(projectId);
        record.setFileName("test.pdf");

        doNothing().when(projectAccessService).assertAction(projectId, DocProjectAction.UPLOAD_DOCUMENT);
        when(documentAssembler.toEntity(bo)).thenReturn(record);

        service.recordUpload(bo);

        verify(projectAccessService).assertAction(projectId, DocProjectAction.UPLOAD_DOCUMENT);
        verify(documentAssembler).toEntity(bo);
        verify(baseMapper).insert(any(DocDocumentRecord.class));

        ArgumentCaptor<DocDocumentRecord> captor = ArgumentCaptor.forClass(DocDocumentRecord.class);
        verify(baseMapper).insert(captor.capture());
        DocDocumentRecord inserted = captor.getValue();
        assertEquals(DocDocumentSourceType.UPLOAD.getCode(), inserted.getSourceType());
        assertEquals(DocDocumentStatus.GENERATED.getCode(), inserted.getStatus());
        assertNotNull(inserted.getGeneratedAt());
    }

    // ==================== recordPluginGenerated ====================

    @Test
    void recordPluginGenerated_shouldCreateRecordWithPluginSourceType() {
        Long projectId = 100L;
        String pluginId = "pdf-generator";
        Long nodeInstanceId = 200L;
        PluginResult.GeneratedFile file = PluginResult.GeneratedFile.builder()
            .fileName("report.pdf")
            .nasPath("/plugins/report.pdf")
            .ossId(54321L)
            .build();

        service.recordPluginGenerated(projectId, pluginId, nodeInstanceId, file);

        verify(baseMapper).insert(any(DocDocumentRecord.class));

        ArgumentCaptor<DocDocumentRecord> captor = ArgumentCaptor.forClass(DocDocumentRecord.class);
        verify(baseMapper).insert(captor.capture());
        DocDocumentRecord inserted = captor.getValue();
        assertEquals(projectId, inserted.getProjectId());
        assertEquals(nodeInstanceId, inserted.getNodeInstanceId());
        assertEquals(pluginId, inserted.getPluginId());
        assertEquals(DocDocumentSourceType.PLUGIN.getCode(), inserted.getSourceType());
        assertEquals("report.pdf", inserted.getFileName());
        assertEquals("/plugins/report.pdf", inserted.getNasPath());
        assertEquals(54321L, inserted.getOssId());
        assertEquals(DocDocumentStatus.GENERATED.getCode(), inserted.getStatus());
        assertNotNull(inserted.getGeneratedAt());
    }

    @Test
    void markLatestUniquePluginArtifactsObsolete_shouldOnlyObsoleteGeneratedRecordsForSamePlugin() {
        Long projectId = 100L;
        String pluginId = "text-export";
        Long nodeInstanceId = 300L;
        DocDocumentRecord samePlugin = createRecord(1L, projectId, DocDocumentStatus.GENERATED.getCode());
        samePlugin.setNodeInstanceId(nodeInstanceId);
        samePlugin.setPluginId(pluginId);
        DocDocumentRecord failedPlugin = createRecord(2L, projectId, DocDocumentStatus.FAILED.getCode());
        failedPlugin.setNodeInstanceId(nodeInstanceId);
        failedPlugin.setPluginId(pluginId);

        when(baseMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(samePlugin, failedPlugin));

        service.markLatestUniquePluginArtifactsObsolete(projectId, pluginId, nodeInstanceId);

        verify(baseMapper).selectList(any(LambdaQueryWrapper.class));
        verify(baseMapper).updateById(samePlugin);
        verify(baseMapper, never()).updateById(failedPlugin);
        assertEquals(DocDocumentStatus.OBSOLETE.getCode(), samePlugin.getStatus());
        assertEquals(DocDocumentStatus.FAILED.getCode(), failedPlugin.getStatus());
    }

    @Test
    void markLatestUniquePluginArtifactsObsolete_shouldKeepGeneratedRecordsFromOtherNodeContext() {
        Long projectId = 100L;
        String pluginId = "text-export";
        DocDocumentRecord currentNodeRecord = createRecord(1L, projectId, DocDocumentStatus.GENERATED.getCode());
        currentNodeRecord.setNodeInstanceId(300L);
        currentNodeRecord.setPluginId(pluginId);
        DocDocumentRecord otherNodeRecord = createRecord(2L, projectId, DocDocumentStatus.GENERATED.getCode());
        otherNodeRecord.setNodeInstanceId(301L);
        otherNodeRecord.setPluginId(pluginId);

        when(baseMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(currentNodeRecord));

        service.markLatestUniquePluginArtifactsObsolete(projectId, pluginId, 300L);

        verify(baseMapper).updateById(currentNodeRecord);
        verify(baseMapper, never()).updateById(otherNodeRecord);
        assertEquals(DocDocumentStatus.OBSOLETE.getCode(), currentNodeRecord.getStatus());
        assertEquals(DocDocumentStatus.GENERATED.getCode(), otherNodeRecord.getStatus());
    }

    // ==================== markObsoleteById ====================

    @Test
    void markObsoleteById_shouldThrowWhenRecordNotFound() {
        Long id = 999L;
        when(baseMapper.selectById(id)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class, () -> service.markObsoleteById(id));

        assertEquals("文档记录不存在", ex.getMessage());
    }

    @Test
    void markObsoleteById_shouldMarkRecordAsObsolete() {
        Long id = 1L;
        Long projectId = 100L;
        DocDocumentRecord record = createRecord(id, projectId, DocDocumentStatus.GENERATED.getCode());

        when(baseMapper.selectById(id)).thenReturn(record);
        doNothing().when(projectAccessService).assertAction(projectId, DocProjectAction.EDIT_PROJECT);

        service.markObsoleteById(id);

        verify(projectAccessService).assertAction(projectId, DocProjectAction.EDIT_PROJECT);
        verify(baseMapper).updateById(any(DocDocumentRecord.class));

        ArgumentCaptor<DocDocumentRecord> captor = ArgumentCaptor.forClass(DocDocumentRecord.class);
        verify(baseMapper).updateById(captor.capture());
        assertEquals(DocDocumentStatus.OBSOLETE.getCode(), captor.getValue().getStatus());
    }

    // ==================== markObsoleteByProjectId ====================

    @Test
    void markObsoleteByProjectId_shouldMarkAllNonArchivedRecords() {
        Long projectId = 100L;
        DocDocumentRecord record1 = createRecord(1L, projectId, DocDocumentStatus.GENERATED.getCode());
        DocDocumentRecord record2 = createRecord(2L, projectId, DocDocumentStatus.PENDING.getCode());
        DocDocumentRecord record3 = createRecord(3L, projectId, DocDocumentStatus.RUNNING.getCode());

        doNothing().when(projectAccessService).assertAction(projectId, DocProjectAction.EDIT_PROJECT);
        when(baseMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(record1, record2, record3));

        service.markObsoleteByProjectId(projectId);

        verify(projectAccessService).assertAction(projectId, DocProjectAction.EDIT_PROJECT);
        // updateById is called for each record (3 times)
        verify(baseMapper).updateById(record1);
        verify(baseMapper).updateById(record2);
        verify(baseMapper).updateById(record3);
        assertEquals(DocDocumentStatus.OBSOLETE.getCode(), record1.getStatus());
        assertEquals(DocDocumentStatus.OBSOLETE.getCode(), record2.getStatus());
        assertEquals(DocDocumentStatus.OBSOLETE.getCode(), record3.getStatus());
    }

    // ==================== listPendingCreatedBeforeByProjectIds ====================

    @Test
    void listPendingCreatedBeforeByProjectIds_shouldReturnEmptyWhenProjectIdsIsNull() {
        Date cutoffTime = new Date();

        List<DocDocumentRecord> result = service.listPendingCreatedBeforeByProjectIds(null, cutoffTime);

        assertTrue(result.isEmpty());
        verify(baseMapper, never()).selectList(any(LambdaQueryWrapper.class));
    }

    @Test
    void listPendingCreatedBeforeByProjectIds_shouldReturnEmptyWhenProjectIdsIsEmpty() {
        Date cutoffTime = new Date();

        List<DocDocumentRecord> result = service.listPendingCreatedBeforeByProjectIds(List.of(), cutoffTime);

        assertTrue(result.isEmpty());
        verify(baseMapper, never()).selectList(any(LambdaQueryWrapper.class));
    }

    @Test
    void listPendingCreatedBeforeByProjectIds_shouldQueryWhenProjectIdsProvided() {
        List<Long> projectIds = List.of(100L, 200L);
        Date cutoffTime = new Date();
        DocDocumentRecord record = createRecord(1L, 100L, DocDocumentStatus.PENDING.getCode());

        when(baseMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(record));

        List<DocDocumentRecord> result = service.listPendingCreatedBeforeByProjectIds(projectIds, cutoffTime);

        assertEquals(1, result.size());
        verify(baseMapper).selectList(any(LambdaQueryWrapper.class));
    }

    // ==================== Helper Methods ====================

    private DocDocumentRecord createRecord(Long id, Long projectId, String status) {
        DocDocumentRecord record = new DocDocumentRecord();
        record.setId(id);
        record.setProjectId(projectId);
        record.setStatus(status);
        record.setFileName("test.pdf");
        record.setNasPath("/docs/test.pdf");
        return record;
    }

    private DocDocumentRecordVo createVo(Long id, Long projectId) {
        DocDocumentRecordVo vo = new DocDocumentRecordVo();
        vo.setId(id);
        vo.setProjectId(projectId);
        vo.setFileName("test.pdf");
        vo.setNasPath("/docs/test.pdf");
        return vo;
    }

    private static void initTableInfo(Class<?> entityClass) {
        if (TableInfoHelper.getTableInfo(entityClass) == null) {
            TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), "test"), entityClass);
        }
    }
}
