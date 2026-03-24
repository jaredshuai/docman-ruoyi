package org.dromara.docman.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.docman.application.port.out.ProcessEnginePort;
import org.dromara.docman.domain.entity.DocProcessConfig;
import org.dromara.docman.domain.enums.DocProcessConfigStatus;
import org.dromara.docman.domain.enums.DocProjectAction;
import org.dromara.docman.mapper.DocProcessConfigMapper;
import org.dromara.docman.service.IDocProjectAccessService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("dev")
@Tag("prod")
@Tag("local")
class DocProcessServiceImplTest {

    @BeforeAll
    static void initTableInfo() {
        initTableInfo(DocProcessConfig.class);
    }

    @Mock
    private DocProcessConfigMapper configMapper;

    @Mock
    private ProcessEnginePort processEnginePort;

    @Mock
    private IDocProjectAccessService projectAccessService;

    @InjectMocks
    private DocProcessServiceImpl service;

    // ========== bindProcess tests ==========

    @Test
    void shouldRejectBindProcessWhenConfigAlreadyExists() {
        Long projectId = 100L;
        Long definitionId = 200L;
        DocProcessConfig existingConfig = createConfig(1L, projectId, definitionId, DocProcessConfigStatus.PENDING);

        when(configMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(existingConfig);

        ServiceException ex = assertThrows(ServiceException.class,
            () -> service.bindProcess(projectId, definitionId));

        assertEquals("该项目已绑定流程，请先解绑", ex.getMessage());
        verify(projectAccessService).assertAction(projectId, DocProjectAction.BIND_PROCESS);
        verify(projectAccessService).assertAction(projectId, DocProjectAction.VIEW_PROCESS);
    }

    @Test
    void shouldBindProcessWhenNoExistingConfig() {
        Long projectId = 100L;
        Long definitionId = 200L;

        when(configMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        when(configMapper.insert(any(DocProcessConfig.class))).thenReturn(1);

        service.bindProcess(projectId, definitionId);

        verify(projectAccessService).assertAction(projectId, DocProjectAction.BIND_PROCESS);
        verify(projectAccessService).assertAction(projectId, DocProjectAction.VIEW_PROCESS);
        verify(configMapper).insert(any(DocProcessConfig.class));
    }

    // ========== startProcess tests ==========

    @Test
    void shouldRejectStartProcessWhenNoConfigBound() {
        Long projectId = 100L;

        when(configMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class,
            () -> service.startProcess(projectId));

        assertEquals("该项目未绑定流程", ex.getMessage());
        verify(projectAccessService).assertAction(projectId, DocProjectAction.START_PROCESS);
        verify(projectAccessService).assertAction(projectId, DocProjectAction.VIEW_PROCESS);
    }

    @Test
    void shouldStartProcessSuccessfully() {
        Long projectId = 100L;
        Long definitionId = 200L;
        Long instanceId = 300L;
        Long userId = 999L;
        DocProcessConfig config = createConfig(1L, projectId, definitionId, DocProcessConfigStatus.PENDING);

        try (MockedStatic<LoginHelper> loginHelper = mockStatic(LoginHelper.class)) {
            loginHelper.when(LoginHelper::getUserId).thenReturn(userId);
            when(configMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(config);
            when(processEnginePort.startProcess(definitionId, String.valueOf(projectId), userId)).thenReturn(instanceId);
            when(configMapper.updateById(any(DocProcessConfig.class))).thenReturn(1);

            Long result = service.startProcess(projectId);

            assertEquals(instanceId, result);
            verify(projectAccessService).assertAction(projectId, DocProjectAction.START_PROCESS);
            verify(projectAccessService).assertAction(projectId, DocProjectAction.VIEW_PROCESS);
            verify(processEnginePort).startProcess(definitionId, String.valueOf(projectId), userId);
            verify(configMapper).updateById(any(DocProcessConfig.class));
        }
    }

    @Test
    void shouldRejectStartProcessWhenCompleted() {
        Long projectId = 100L;
        Long definitionId = 200L;
        DocProcessConfig config = createConfig(1L, projectId, definitionId, DocProcessConfigStatus.COMPLETED);

        when(configMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(config);

        ServiceException ex = assertThrows(ServiceException.class,
            () -> service.startProcess(projectId));

        assertEquals("非法状态变更: completed -> running", ex.getMessage());
        verify(projectAccessService).assertAction(projectId, DocProjectAction.START_PROCESS);
        verify(projectAccessService).assertAction(projectId, DocProjectAction.VIEW_PROCESS);
    }

    // ========== getByProjectId tests ==========

    @Test
    void shouldReturnConfigByProjectId() {
        Long projectId = 100L;
        DocProcessConfig config = createConfig(1L, projectId, 200L, DocProcessConfigStatus.PENDING);

        when(configMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(config);

        DocProcessConfig result = service.getByProjectId(projectId);

        assertEquals(config, result);
        verify(projectAccessService).assertAction(projectId, DocProjectAction.VIEW_PROCESS);
    }

    @Test
    void shouldReturnNullWhenNoConfigFound() {
        Long projectId = 100L;

        when(configMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        DocProcessConfig result = service.getByProjectId(projectId);

        assertNull(result);
        verify(projectAccessService).assertAction(projectId, DocProjectAction.VIEW_PROCESS);
    }

    // ========== helper methods ==========

    private DocProcessConfig createConfig(Long id, Long projectId, Long definitionId, DocProcessConfigStatus status) {
        DocProcessConfig config = new DocProcessConfig();
        config.setId(id);
        config.setProjectId(projectId);
        config.setDefinitionId(definitionId);
        config.setStatus(status.getCode());
        return config;
    }

    private static void initTableInfo(Class<?> entityClass) {
        if (TableInfoHelper.getTableInfo(entityClass) == null) {
            TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), "test"), entityClass);
        }
    }
}