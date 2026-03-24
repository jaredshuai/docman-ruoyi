package org.dromara.docman.infrastructure.log;

import org.dromara.docman.domain.entity.DocPluginExecutionLog;
import org.dromara.docman.mapper.DocPluginExecutionLogMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

/**
 * MybatisPluginExecutionLogAdapter 单元测试
 * 直接测试 save() 方法对 mapper 的委托，不依赖 Spring 上下文
 */
@Tag("local")
@ExtendWith(MockitoExtension.class)
class MybatisPluginExecutionLogAdapterTest {

    @Mock
    private DocPluginExecutionLogMapper pluginExecutionLogMapper;

    private MybatisPluginExecutionLogAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new MybatisPluginExecutionLogAdapter(pluginExecutionLogMapper);
    }

    @Test
    void save_shouldDelegateToMapperInsert() {
        // Given
        DocPluginExecutionLog executionLog = new DocPluginExecutionLog();
        executionLog.setId(1L);
        executionLog.setProjectId(100L);
        executionLog.setPluginId("test-plugin");
        executionLog.setPluginName("测试插件");
        executionLog.setStatus("SUCCESS");

        // When
        adapter.save(executionLog);

        // Then
        verify(pluginExecutionLogMapper, times(1)).insert(executionLog);
    }

    @Test
    void save_shouldDelegateToMapperInsert_withMinimalFields() {
        // Given
        DocPluginExecutionLog executionLog = new DocPluginExecutionLog();
        // 只设置必要字段，不设置可选字段

        // When
        adapter.save(executionLog);

        // Then
        verify(pluginExecutionLogMapper, times(1)).insert(executionLog);
    }
}