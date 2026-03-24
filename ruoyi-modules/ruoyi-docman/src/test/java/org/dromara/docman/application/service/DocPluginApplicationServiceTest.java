package org.dromara.docman.application.service;

import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.docman.application.assembler.DocPluginAssembler;
import org.dromara.docman.domain.bo.DocPluginTriggerBo;
import org.dromara.docman.domain.entity.DocProcessConfig;
import org.dromara.docman.domain.enums.DocProcessConfigStatus;
import org.dromara.docman.domain.vo.DocPluginExecutionLogVo;
import org.dromara.docman.domain.vo.DocPluginInfoVo;
import org.dromara.docman.plugin.DocumentPlugin;
import org.dromara.docman.plugin.PluginRegistry;
import org.dromara.docman.service.IDocProcessConfigService;
import org.dromara.docman.service.IDocPluginExecutionLogService;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("dev")
@Tag("prod")
@Tag("local")
class DocPluginApplicationServiceTest {

    @Mock
    private PluginRegistry pluginRegistry;

    @Mock
    private IDocPluginExecutionLogService pluginExecutionLogService;

    @Mock
    private IDocProcessConfigService processConfigService;

    @Mock
    private DocWorkflowNodeApplicationService workflowNodeApplicationService;

    @Mock
    private DocPluginAssembler pluginAssembler;

    @InjectMocks
    private DocPluginApplicationService applicationService;

    @Test
    void shouldMapPluginsToInfoVoList_whenListPlugins() {
        DocumentPlugin plugin = mock(DocumentPlugin.class);

        DocPluginInfoVo expectedVo = new DocPluginInfoVo();
        expectedVo.setPluginId("test-plugin");
        expectedVo.setPluginName("Test Plugin");
        expectedVo.setPluginType("excel_fill");

        when(pluginRegistry.getAllPlugins()).thenReturn(Map.of("test-plugin", plugin));
        when(pluginAssembler.toInfoVo(plugin)).thenReturn(expectedVo);

        List<DocPluginInfoVo> result = applicationService.listPlugins();

        assertEquals(1, result.size());
        assertEquals("test-plugin", result.get(0).getPluginId());
        assertEquals("Test Plugin", result.get(0).getPluginName());
        verify(pluginAssembler).toInfoVo(plugin);
    }

    @Test
    void shouldReturnEmptyList_whenNoPluginsRegistered() {
        when(pluginRegistry.getAllPlugins()).thenReturn(Collections.emptyMap());

        List<DocPluginInfoVo> result = applicationService.listPlugins();

        assertTrue(result.isEmpty());
    }

    @Test
    void shouldDelegateToService_whenListExecutionLogs() {
        PageQuery pageQuery = new PageQuery(10, 1);
        TableDataInfo<DocPluginExecutionLogVo> expected = new TableDataInfo<>();
        when(pluginExecutionLogService.queryPageList(1L, 2L, "node1", "plugin1", pageQuery))
            .thenReturn(expected);

        TableDataInfo<DocPluginExecutionLogVo> result = applicationService.listExecutionLogs(
            1L, 2L, "node1", "plugin1", pageQuery);

        assertEquals(expected, result);
        verify(pluginExecutionLogService).queryPageList(1L, 2L, "node1", "plugin1", pageQuery);
    }

    @Test
    void shouldTriggerPlugins_whenProcessConfigIsRunning() {
        DocProcessConfig config = new DocProcessConfig();
        config.setId(1L);
        config.setStatus(DocProcessConfigStatus.RUNNING.getCode());

        DocPluginTriggerBo bo = new DocPluginTriggerBo();
        bo.setProcessInstanceId(100L);
        bo.setNodeCode("node1");

        when(processConfigService.queryByInstanceId(100L)).thenReturn(config);

        applicationService.triggerPlugin(bo);

        verify(workflowNodeApplicationService).triggerPlugins(config, "node1");
    }

    @Test
    void shouldThrowException_whenProcessConfigIsNull() {
        DocPluginTriggerBo bo = new DocPluginTriggerBo();
        bo.setProcessInstanceId(100L);

        when(processConfigService.queryByInstanceId(100L)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class,
            () -> applicationService.triggerPlugin(bo));

        assertEquals("流程实例不存在或未关联文档流程配置", ex.getMessage());
    }

    @Test
    void shouldThrowException_whenProcessConfigNotRunning() {
        DocProcessConfig config = new DocProcessConfig();
        config.setId(1L);
        config.setStatus(DocProcessConfigStatus.PENDING.getCode());

        DocPluginTriggerBo bo = new DocPluginTriggerBo();
        bo.setProcessInstanceId(100L);

        when(processConfigService.queryByInstanceId(100L)).thenReturn(config);

        ServiceException ex = assertThrows(ServiceException.class,
            () -> applicationService.triggerPlugin(bo));

        assertEquals("仅运行中的流程实例允许手动触发插件", ex.getMessage());
    }

    @Test
    void shouldThrowException_whenProcessConfigCompleted() {
        DocProcessConfig config = new DocProcessConfig();
        config.setId(1L);
        config.setStatus(DocProcessConfigStatus.COMPLETED.getCode());

        DocPluginTriggerBo bo = new DocPluginTriggerBo();
        bo.setProcessInstanceId(100L);

        when(processConfigService.queryByInstanceId(100L)).thenReturn(config);

        ServiceException ex = assertThrows(ServiceException.class,
            () -> applicationService.triggerPlugin(bo));

        assertEquals("仅运行中的流程实例允许手动触发插件", ex.getMessage());
    }
}