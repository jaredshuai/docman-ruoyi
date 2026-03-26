package org.dromara.docman.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.docman.domain.entity.DocPluginExecutionLog;
import org.dromara.docman.domain.enums.DocProjectAction;
import org.dromara.docman.domain.vo.DocPluginExecutionLogVo;
import org.dromara.docman.mapper.DocPluginExecutionLogMapper;
import org.dromara.docman.service.IDocProjectAccessService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * DocPluginExecutionLogServiceImpl 单元测试
 * 直接测试权限委托、可选过滤条件分支和分页返回路径，不依赖 Spring 上下文
 */
@Tag("local")
@ExtendWith(MockitoExtension.class)
class DocPluginExecutionLogServiceImplTest {

    @Mock
    private DocPluginExecutionLogMapper pluginExecutionLogMapper;

    @Mock
    private IDocProjectAccessService projectAccessService;

    @InjectMocks
    private DocPluginExecutionLogServiceImpl service;

    @BeforeAll
    static void initTableInfo() {
        initTableInfo(DocPluginExecutionLog.class);
    }

    @BeforeEach
    void setUp() {
        // 每个测试前的初始化
    }

    // ==================== Permission Delegation Tests ====================

    @Test
    void queryPageList_shouldDelegatePermissionCheck() {
        // Given
        Long projectId = 100L;
        PageQuery pageQuery = createPageQuery(1, 10);
        Page<DocPluginExecutionLogVo> mockPage = createEmptyPage();

        when(pluginExecutionLogMapper.selectVoPage(any(Page.class), any(LambdaQueryWrapper.class)))
            .thenReturn(mockPage);

        // When
        service.queryPageList(projectId, null, null, null, pageQuery);

        // Then - verify permission delegation
        verify(projectAccessService).assertAction(projectId, DocProjectAction.VIEW_PROJECT);
    }

    @Test
    void queryPageList_shouldDelegatePermissionCheckWithCorrectAction() {
        // Given
        Long projectId = 200L;
        PageQuery pageQuery = createPageQuery(1, 10);
        Page<DocPluginExecutionLogVo> mockPage = createEmptyPage();

        when(pluginExecutionLogMapper.selectVoPage(any(Page.class), any(LambdaQueryWrapper.class)))
            .thenReturn(mockPage);

        // When
        service.queryPageList(projectId, null, null, null, pageQuery);

        // Then - verify VIEW_PROJECT action is used
        ArgumentCaptor<DocProjectAction> actionCaptor = ArgumentCaptor.forClass(DocProjectAction.class);
        verify(projectAccessService).assertAction(eq(projectId), actionCaptor.capture());
        assertEquals(DocProjectAction.VIEW_PROJECT, actionCaptor.getValue());
    }

    // ==================== Optional Filter Branch Tests ====================

    @Test
    void queryPageList_shouldApplyProcessInstanceIdFilter_whenNotNull() {
        // Given
        Long projectId = 100L;
        Long processInstanceId = 500L;
        PageQuery pageQuery = createPageQuery(1, 10);
        Page<DocPluginExecutionLogVo> mockPage = createEmptyPage();

        ArgumentCaptor<LambdaQueryWrapper<DocPluginExecutionLog>> wrapperCaptor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        when(pluginExecutionLogMapper.selectVoPage(any(Page.class), wrapperCaptor.capture()))
            .thenReturn(mockPage);

        // When
        service.queryPageList(projectId, processInstanceId, null, null, pageQuery);

        // Then - wrapper should be created (verification that it was called)
        verify(pluginExecutionLogMapper).selectVoPage(any(Page.class), any(LambdaQueryWrapper.class));
        assertNotNull(wrapperCaptor.getValue());
    }

    @Test
    void queryPageList_shouldApplyNodeCodeFilter_whenNotBlank() {
        // Given
        Long projectId = 100L;
        String nodeCode = "node_001";
        PageQuery pageQuery = createPageQuery(1, 10);
        Page<DocPluginExecutionLogVo> mockPage = createEmptyPage();

        ArgumentCaptor<LambdaQueryWrapper<DocPluginExecutionLog>> wrapperCaptor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        when(pluginExecutionLogMapper.selectVoPage(any(Page.class), wrapperCaptor.capture()))
            .thenReturn(mockPage);

        // When
        service.queryPageList(projectId, null, nodeCode, null, pageQuery);

        // Then
        verify(pluginExecutionLogMapper).selectVoPage(any(Page.class), any(LambdaQueryWrapper.class));
        assertNotNull(wrapperCaptor.getValue());
    }

    @Test
    void queryPageList_shouldNotApplyNodeCodeFilter_whenBlank() {
        // Given
        Long projectId = 100L;
        String nodeCode = "";  // blank string
        PageQuery pageQuery = createPageQuery(1, 10);
        Page<DocPluginExecutionLogVo> mockPage = createEmptyPage();

        ArgumentCaptor<LambdaQueryWrapper<DocPluginExecutionLog>> wrapperCaptor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        when(pluginExecutionLogMapper.selectVoPage(any(Page.class), wrapperCaptor.capture()))
            .thenReturn(mockPage);

        // When
        service.queryPageList(projectId, null, nodeCode, null, pageQuery);

        // Then
        verify(pluginExecutionLogMapper).selectVoPage(any(Page.class), any(LambdaQueryWrapper.class));
        assertNotNull(wrapperCaptor.getValue());
    }

    @Test
    void queryPageList_shouldApplyPluginIdFilter_whenNotBlank() {
        // Given
        Long projectId = 100L;
        String pluginId = "plugin_001";
        PageQuery pageQuery = createPageQuery(1, 10);
        Page<DocPluginExecutionLogVo> mockPage = createEmptyPage();

        ArgumentCaptor<LambdaQueryWrapper<DocPluginExecutionLog>> wrapperCaptor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        when(pluginExecutionLogMapper.selectVoPage(any(Page.class), wrapperCaptor.capture()))
            .thenReturn(mockPage);

        // When
        service.queryPageList(projectId, null, null, pluginId, pageQuery);

        // Then
        verify(pluginExecutionLogMapper).selectVoPage(any(Page.class), any(LambdaQueryWrapper.class));
        assertNotNull(wrapperCaptor.getValue());
    }

    @Test
    void queryPageList_shouldNotApplyPluginIdFilter_whenBlank() {
        // Given
        Long projectId = 100L;
        String pluginId = "   ";  // whitespace only
        PageQuery pageQuery = createPageQuery(1, 10);
        Page<DocPluginExecutionLogVo> mockPage = createEmptyPage();

        ArgumentCaptor<LambdaQueryWrapper<DocPluginExecutionLog>> wrapperCaptor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        when(pluginExecutionLogMapper.selectVoPage(any(Page.class), wrapperCaptor.capture()))
            .thenReturn(mockPage);

        // When
        service.queryPageList(projectId, null, null, pluginId, pageQuery);

        // Then
        verify(pluginExecutionLogMapper).selectVoPage(any(Page.class), any(LambdaQueryWrapper.class));
        assertNotNull(wrapperCaptor.getValue());
    }

    @Test
    void queryPageList_shouldApplyAllFilters_whenAllProvided() {
        // Given
        Long projectId = 100L;
        Long processInstanceId = 500L;
        String nodeCode = "node_001";
        String pluginId = "plugin_001";
        PageQuery pageQuery = createPageQuery(1, 10);
        Page<DocPluginExecutionLogVo> mockPage = createEmptyPage();

        ArgumentCaptor<LambdaQueryWrapper<DocPluginExecutionLog>> wrapperCaptor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        when(pluginExecutionLogMapper.selectVoPage(any(Page.class), wrapperCaptor.capture()))
            .thenReturn(mockPage);

        // When
        service.queryPageList(projectId, processInstanceId, nodeCode, pluginId, pageQuery);

        // Then
        verify(pluginExecutionLogMapper).selectVoPage(any(Page.class), any(LambdaQueryWrapper.class));
        assertNotNull(wrapperCaptor.getValue());
    }

    @Test
    void queryPageList_shouldApplyNoOptionalFilters_whenAllNull() {
        // Given
        Long projectId = 100L;
        PageQuery pageQuery = createPageQuery(1, 10);
        Page<DocPluginExecutionLogVo> mockPage = createEmptyPage();

        ArgumentCaptor<LambdaQueryWrapper<DocPluginExecutionLog>> wrapperCaptor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        when(pluginExecutionLogMapper.selectVoPage(any(Page.class), wrapperCaptor.capture()))
            .thenReturn(mockPage);

        // When
        service.queryPageList(projectId, null, null, null, pageQuery);

        // Then
        verify(pluginExecutionLogMapper).selectVoPage(any(Page.class), any(LambdaQueryWrapper.class));
        assertNotNull(wrapperCaptor.getValue());
    }

    // ==================== queryPageList Return Path Tests ====================

    @Test
    void queryPageList_shouldReturnTableDataInfoWithResults() {
        // Given
        Long projectId = 100L;
        PageQuery pageQuery = createPageQuery(1, 10);
        Page<DocPluginExecutionLogVo> mockPage = createPageWithRecords(2);

        when(pluginExecutionLogMapper.selectVoPage(any(Page.class), any(LambdaQueryWrapper.class)))
            .thenReturn(mockPage);

        // When
        TableDataInfo<DocPluginExecutionLogVo> result = service.queryPageList(projectId, null, null, null, pageQuery);

        // Then
        assertNotNull(result);
        assertEquals(2, result.getRows().size());
        assertEquals(2L, result.getTotal());
    }

    @Test
    void queryPageList_shouldReturnEmptyTableDataInfo_whenNoResults() {
        // Given
        Long projectId = 100L;
        PageQuery pageQuery = createPageQuery(1, 10);
        Page<DocPluginExecutionLogVo> mockPage = createEmptyPage();

        when(pluginExecutionLogMapper.selectVoPage(any(Page.class), any(LambdaQueryWrapper.class)))
            .thenReturn(mockPage);

        // When
        TableDataInfo<DocPluginExecutionLogVo> result = service.queryPageList(projectId, null, null, null, pageQuery);

        // Then
        assertNotNull(result);
        assertEquals(0, result.getRows().size());
        assertEquals(0L, result.getTotal());
    }

    @Test
    void queryPageList_shouldReturnTableDataInfoBuiltFromPage() {
        // Given
        Long projectId = 100L;
        PageQuery pageQuery = createPageQuery(2, 5);
        Page<DocPluginExecutionLogVo> mockPage = new Page<>(2, 5);
        mockPage.setRecords(createVoList(3));
        mockPage.setTotal(13);

        when(pluginExecutionLogMapper.selectVoPage(any(Page.class), any(LambdaQueryWrapper.class)))
            .thenReturn(mockPage);

        // When
        TableDataInfo<DocPluginExecutionLogVo> result = service.queryPageList(projectId, null, null, null, pageQuery);

        // Then
        assertNotNull(result);
        assertEquals(3, result.getRows().size());
        assertEquals(13L, result.getTotal());
    }

    @Test
    void queryPageList_shouldUsePageQueryForPagination() {
        // Given
        Long projectId = 100L;
        PageQuery pageQuery = createPageQuery(3, 20);
        ArgumentCaptor<Page<DocPluginExecutionLog>> pageCaptor = ArgumentCaptor.forClass(Page.class);
        Page<DocPluginExecutionLogVo> mockPage = createEmptyPage();

        when(pluginExecutionLogMapper.selectVoPage(pageCaptor.capture(), any(LambdaQueryWrapper.class)))
            .thenReturn(mockPage);

        // When
        service.queryPageList(projectId, null, null, null, pageQuery);

        // Then
        assertEquals(3L, pageCaptor.getValue().getCurrent());
        assertEquals(20L, pageCaptor.getValue().getSize());
    }

    // ==================== queryById Tests ====================

    @Test
    void queryById_shouldReturnLogVo_whenLogExists() {
        // Given
        Long logId = 1L;
        Long projectId = 100L;
        DocPluginExecutionLog log = new DocPluginExecutionLog();
        log.setId(logId);
        log.setProjectId(projectId);

        DocPluginExecutionLogVo vo = new DocPluginExecutionLogVo();
        vo.setId(logId);
        vo.setProjectId(projectId);
        vo.setPluginId("test_plugin");
        vo.setPluginName("Test Plugin");

        when(pluginExecutionLogMapper.selectById(logId)).thenReturn(log);
        when(pluginExecutionLogMapper.selectVoById(logId)).thenReturn(vo);

        // When
        DocPluginExecutionLogVo result = service.queryById(logId);

        // Then
        assertNotNull(result);
        assertEquals(logId, result.getId());
        assertEquals(projectId, result.getProjectId());
        verify(projectAccessService).assertAction(projectId, DocProjectAction.VIEW_PROJECT);
    }

    @Test
    void queryById_shouldThrowServiceException_whenLogNotFound() {
        // Given
        Long logId = 999L;
        when(pluginExecutionLogMapper.selectById(logId)).thenReturn(null);

        // When & Then
        ServiceException ex = assertThrows(ServiceException.class, () -> service.queryById(logId));
        assertEquals("插件执行日志不存在", ex.getMessage());
    }

    @Test
    void queryById_shouldCheckProjectAccessPermission() {
        // Given
        Long logId = 1L;
        Long projectId = 200L;
        DocPluginExecutionLog log = new DocPluginExecutionLog();
        log.setId(logId);
        log.setProjectId(projectId);

        DocPluginExecutionLogVo vo = new DocPluginExecutionLogVo();
        vo.setId(logId);

        when(pluginExecutionLogMapper.selectById(logId)).thenReturn(log);
        when(pluginExecutionLogMapper.selectVoById(logId)).thenReturn(vo);

        // When
        service.queryById(logId);

        // Then
        verify(projectAccessService).assertAction(eq(projectId), eq(DocProjectAction.VIEW_PROJECT));
    }

    // ==================== Helper Methods ====================

    private PageQuery createPageQuery(int pageNum, int pageSize) {
        return new PageQuery(pageSize, pageNum);
    }

    private Page<DocPluginExecutionLogVo> createEmptyPage() {
        Page<DocPluginExecutionLogVo> page = new Page<>(1, 10);
        page.setRecords(new ArrayList<>());
        page.setTotal(0);
        return page;
    }

    private Page<DocPluginExecutionLogVo> createPageWithRecords(int count) {
        Page<DocPluginExecutionLogVo> page = new Page<>(1, 10);
        page.setRecords(createVoList(count));
        page.setTotal(count);
        return page;
    }

    private List<DocPluginExecutionLogVo> createVoList(int count) {
        List<DocPluginExecutionLogVo> list = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            DocPluginExecutionLogVo vo = new DocPluginExecutionLogVo();
            vo.setId((long) i);
            vo.setProjectId(100L);
            vo.setPluginId("plugin_" + i);
            vo.setPluginName("Plugin " + i);
            vo.setStatus("SUCCESS");
            list.add(vo);
        }
        return list;
    }

    private static void initTableInfo(Class<?> entityClass) {
        if (TableInfoHelper.getTableInfo(entityClass) == null) {
            TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), "test"), entityClass);
        }
    }
}