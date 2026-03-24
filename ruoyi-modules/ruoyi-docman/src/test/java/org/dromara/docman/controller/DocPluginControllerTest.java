package org.dromara.docman.controller;

import org.dromara.common.core.domain.R;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.docman.application.service.DocPluginApplicationService;
import org.dromara.docman.domain.bo.DocPluginTriggerBo;
import org.dromara.docman.domain.vo.DocPluginExecutionLogVo;
import org.dromara.docman.domain.vo.DocPluginInfoVo;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("dev")
@Tag("prod")
@Tag("local")
class DocPluginControllerTest {

    @Mock
    private DocPluginApplicationService pluginApplicationService;

    @Test
    void shouldReturnPluginListFromService() {
        DocPluginController controller = new DocPluginController(pluginApplicationService);
        DocPluginInfoVo vo = new DocPluginInfoVo();
        vo.setPluginId("ai-generator");
        vo.setPluginName("AI生成插件");
        when(pluginApplicationService.listPlugins()).thenReturn(List.of(vo));

        R<List<DocPluginInfoVo>> result = controller.list();

        assertEquals(R.SUCCESS, result.getCode());
        assertEquals(1, result.getData().size());
        assertEquals("ai-generator", result.getData().get(0).getPluginId());
        verify(pluginApplicationService).listPlugins();
    }

    @Test
    void shouldReturnExecutionLogsFromService() {
        DocPluginController controller = new DocPluginController(pluginApplicationService);
        Long projectId = 1L;
        Long processInstanceId = 100L;
        String nodeCode = "node1";
        String pluginId = "ai-generator";
        PageQuery pageQuery = new PageQuery(10, 1);
        DocPluginExecutionLogVo vo = new DocPluginExecutionLogVo();
        vo.setPluginName("AI生成插件");
        TableDataInfo<DocPluginExecutionLogVo> expected = TableDataInfo.build(List.of(vo));
        when(pluginApplicationService.listExecutionLogs(projectId, processInstanceId, nodeCode, pluginId, pageQuery))
            .thenReturn(expected);

        TableDataInfo<DocPluginExecutionLogVo> result = controller.listExecutionLogs(projectId, processInstanceId, nodeCode, pluginId, pageQuery);

        assertEquals(expected, result);
        verify(pluginApplicationService).listExecutionLogs(projectId, processInstanceId, nodeCode, pluginId, pageQuery);
    }

    @Test
    void shouldTriggerPluginAndReturnSuccess() {
        DocPluginController controller = new DocPluginController(pluginApplicationService);
        DocPluginTriggerBo bo = new DocPluginTriggerBo();
        bo.setProcessInstanceId(100L);
        bo.setNodeCode("node1");

        R<Void> result = controller.triggerExecution(bo);

        assertEquals(R.SUCCESS, result.getCode());
        verify(pluginApplicationService).triggerPlugin(bo);
    }

    @Test
    void shouldTriggerPluginWithOnlyProcessInstanceId() {
        DocPluginController controller = new DocPluginController(pluginApplicationService);
        DocPluginTriggerBo bo = new DocPluginTriggerBo();
        bo.setProcessInstanceId(100L);

        R<Void> result = controller.triggerExecution(bo);

        assertEquals(R.SUCCESS, result.getCode());
        verify(pluginApplicationService).triggerPlugin(bo);
    }
}