package org.dromara.docman.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.dromara.docman.domain.entity.DocProcessConfig;
import org.dromara.docman.domain.enums.DocProcessConfigStatus;
import org.dromara.docman.mapper.DocProcessConfigMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("dev")
@Tag("prod")
@Tag("local")
class DocProcessConfigServiceImplTest {

    @BeforeAll
    static void initTableInfo() {
        initTableInfo(DocProcessConfig.class);
    }

    @Mock
    private DocProcessConfigMapper processConfigMapper;

    @InjectMocks
    private DocProcessConfigServiceImpl service;

    // ========== queryByInstanceId tests ==========

    @Test
    void shouldReturnConfigWhenFoundByInstanceId() {
        Long instanceId = 100L;
        DocProcessConfig config = createConfig(1L, 200L, 300L, instanceId, DocProcessConfigStatus.RUNNING);

        when(processConfigMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(config);

        DocProcessConfig result = service.queryByInstanceId(instanceId);

        assertNotNull(result);
        assertEquals(instanceId, result.getInstanceId());
        verify(processConfigMapper).selectOne(any(LambdaQueryWrapper.class));
    }

    @Test
    void shouldReturnNullWhenConfigNotFoundByInstanceId() {
        Long instanceId = 100L;

        when(processConfigMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        DocProcessConfig result = service.queryByInstanceId(instanceId);

        assertNull(result);
        verify(processConfigMapper).selectOne(any(LambdaQueryWrapper.class));
    }

    // ========== listByStatus tests ==========

    @Test
    void shouldReturnConfigsWhenFoundByStatus() {
        DocProcessConfigStatus status = DocProcessConfigStatus.PENDING;
        DocProcessConfig config1 = createConfig(1L, 100L, 200L, null, status);
        DocProcessConfig config2 = createConfig(2L, 101L, 201L, null, status);
        List<DocProcessConfig> expectedList = Arrays.asList(config1, config2);

        when(processConfigMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(expectedList);

        List<DocProcessConfig> result = service.listByStatus(status);

        assertNotNull(result);
        assertEquals(2, result.size());
        verify(processConfigMapper).selectList(any(LambdaQueryWrapper.class));
    }

    @Test
    void shouldReturnEmptyListWhenNoConfigsFoundByStatus() {
        DocProcessConfigStatus status = DocProcessConfigStatus.COMPLETED;

        when(processConfigMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());

        List<DocProcessConfig> result = service.listByStatus(status);

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(processConfigMapper).selectList(any(LambdaQueryWrapper.class));
    }

    // ========== updateStatus tests ==========

    @Test
    void shouldUpdateStatusSuccessfully() {
        Long id = 1L;
        String newStatus = "completed";

        when(processConfigMapper.update(any(), any(LambdaUpdateWrapper.class))).thenReturn(1);

        service.updateStatus(id, newStatus);

        verify(processConfigMapper).update(any(), any(LambdaUpdateWrapper.class));
    }

    @Test
    void shouldUpdateStatusToRunning() {
        Long id = 2L;
        String newStatus = "running";

        when(processConfigMapper.update(any(), any(LambdaUpdateWrapper.class))).thenReturn(1);

        service.updateStatus(id, newStatus);

        verify(processConfigMapper).update(any(), any(LambdaUpdateWrapper.class));
    }

    // ========== helper methods ==========

    private DocProcessConfig createConfig(Long id, Long projectId, Long definitionId, Long instanceId, DocProcessConfigStatus status) {
        DocProcessConfig config = new DocProcessConfig();
        config.setId(id);
        config.setProjectId(projectId);
        config.setDefinitionId(definitionId);
        config.setInstanceId(instanceId);
        config.setStatus(status.getCode());
        return config;
    }

    private static void initTableInfo(Class<?> entityClass) {
        if (TableInfoHelper.getTableInfo(entityClass) == null) {
            TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), "test"), entityClass);
        }
    }
}