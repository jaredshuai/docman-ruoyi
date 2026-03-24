package org.dromara.docman.controller;

import cn.hutool.json.JSONUtil;
import org.dromara.common.core.domain.R;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.docman.application.service.DocNodeDeadlineApplicationService;
import org.dromara.docman.application.service.DocNodeDeadlineQueryApplicationService;
import org.dromara.docman.domain.bo.DocNodeDeadlineBo;
import org.dromara.docman.domain.bo.NodeDurationBo;
import org.dromara.docman.domain.vo.DocNodeDeadlineVo;
import org.dromara.docman.domain.vo.FlowNodeDurationVo;
import org.dromara.warm.flow.core.FlowEngine;
import org.dromara.warm.flow.core.entity.Node;
import org.dromara.warm.flow.core.service.NodeService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("dev")
@Tag("prod")
@Tag("local")
class DocNodeDeadlineControllerTest {

    @Mock
    private DocNodeDeadlineQueryApplicationService nodeDeadlineQueryApplicationService;

    @Mock
    private DocNodeDeadlineApplicationService nodeDeadlineApplicationService;

    @Test
    void shouldDelegateDeadlineListQuery() {
        DocNodeDeadlineController controller = new DocNodeDeadlineController(
            nodeDeadlineQueryApplicationService,
            nodeDeadlineApplicationService
        );
        List<DocNodeDeadlineVo> expected = List.of(new DocNodeDeadlineVo());
        when(nodeDeadlineQueryApplicationService.listByProject(8L)).thenReturn(expected);

        R<List<DocNodeDeadlineVo>> result = controller.list(8L);

        assertEquals(R.SUCCESS, result.getCode());
        assertEquals(expected, result.getData());
    }

    @Test
    void shouldDelegateDeadlineUpdate() {
        DocNodeDeadlineController controller = new DocNodeDeadlineController(
            nodeDeadlineQueryApplicationService,
            nodeDeadlineApplicationService
        );
        DocNodeDeadlineBo bo = new DocNodeDeadlineBo();
        bo.setId(3L);

        R<Void> result = controller.edit(bo);

        assertEquals(R.SUCCESS, result.getCode());
        verify(nodeDeadlineApplicationService).update(bo);
    }

    @Test
    void shouldMapFlowNodesToDurationVoList() {
        DocNodeDeadlineController controller = new DocNodeDeadlineController(
            nodeDeadlineQueryApplicationService,
            nodeDeadlineApplicationService
        );
        NodeService nodeService = mock(NodeService.class);
        List<Node> nodes = List.of(
            mockNode(1L, "N1", "节点一", "{\"durationDays\":5}"),
            mockNode(2L, "N2", "节点二", "{\"extra\":{\"durationDays\":3}}"),
            mockNode(3L, "N3", "节点三", ""),
            mockNode(4L, "N4", "节点四", "{bad-json")
        );
        when(nodeService.getByDefId(66L)).thenReturn(nodes);

        try (MockedStatic<FlowEngine> flowEngine = mockStatic(FlowEngine.class)) {
            flowEngine.when(FlowEngine::nodeService).thenReturn(nodeService);

            R<List<FlowNodeDurationVo>> result = controller.listNodes(66L);

            assertEquals(R.SUCCESS, result.getCode());
            assertEquals(4, result.getData().size());
            assertEquals(5, result.getData().get(0).getDurationDays());
            assertEquals(3, result.getData().get(1).getDurationDays());
            assertEquals(0, result.getData().get(2).getDurationDays());
            assertEquals(0, result.getData().get(3).getDurationDays());
        }
    }

    @Test
    void shouldRejectDurationUpdateWhenNodeDoesNotExist() {
        DocNodeDeadlineController controller = new DocNodeDeadlineController(
            nodeDeadlineQueryApplicationService,
            nodeDeadlineApplicationService
        );
        NodeService nodeService = mock(NodeService.class);
        NodeDurationBo bo = new NodeDurationBo();
        bo.setNodeId(100L);
        bo.setDurationDays(7);
        when(nodeService.getById(100L)).thenReturn(null);

        try (MockedStatic<FlowEngine> flowEngine = mockStatic(FlowEngine.class)) {
            flowEngine.when(FlowEngine::nodeService).thenReturn(nodeService);

            ServiceException ex = assertThrows(ServiceException.class, () -> controller.updateNodeDuration(bo));

            assertEquals("流程节点不存在", ex.getMessage());
        }
    }

    @Test
    void shouldUpdateNodeDurationInExtJson() {
        DocNodeDeadlineController controller = new DocNodeDeadlineController(
            nodeDeadlineQueryApplicationService,
            nodeDeadlineApplicationService
        );
        NodeService nodeService = mock(NodeService.class);
        Node node = mock(Node.class);
        NodeDurationBo bo = new NodeDurationBo();
        bo.setNodeId(9L);
        bo.setDurationDays(12);
        when(node.getExt()).thenReturn("{\"durationDays\":1,\"remark\":\"demo\"}");
        when(nodeService.getById(9L)).thenReturn(node);
        when(nodeService.updateById(node)).thenReturn(true);

        try (MockedStatic<FlowEngine> flowEngine = mockStatic(FlowEngine.class)) {
            flowEngine.when(FlowEngine::nodeService).thenReturn(nodeService);

            R<Void> result = controller.updateNodeDuration(bo);

            assertEquals(R.SUCCESS, result.getCode());
            verify(nodeService).updateById(node);
            ArgumentCaptor<String> extCaptor = ArgumentCaptor.forClass(String.class);
            verify(node).setExt(extCaptor.capture());
            assertEquals(12, JSONUtil.parseObj(extCaptor.getValue()).getInt("durationDays"));
        }
    }

    private Node mockNode(Long id, String code, String name, String ext) {
        Node node = mock(Node.class);
        when(node.getId()).thenReturn(id);
        when(node.getNodeCode()).thenReturn(code);
        when(node.getNodeName()).thenReturn(name);
        when(node.getExt()).thenReturn(ext);
        return node;
    }
}
