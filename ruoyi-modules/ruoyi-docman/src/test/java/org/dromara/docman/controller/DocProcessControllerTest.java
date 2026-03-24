package org.dromara.docman.controller;

import org.dromara.common.core.domain.R;
import org.dromara.docman.application.service.DocProcessApplicationService;
import org.dromara.docman.application.service.DocProcessQueryApplicationService;
import org.dromara.docman.domain.vo.DocProcessConfigVo;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("dev")
@Tag("prod")
@Tag("local")
class DocProcessControllerTest {

    @Mock
    private DocProcessApplicationService processApplicationService;

    @Mock
    private DocProcessQueryApplicationService processQueryApplicationService;

    @Test
    void shouldDelegateProcessBind() {
        DocProcessController controller = new DocProcessController(processApplicationService, processQueryApplicationService);

        R<Void> result = controller.bind(1L, 100L);

        assertEquals(R.SUCCESS, result.getCode());
        verify(processApplicationService).bind(1L, 100L);
    }

    @Test
    void shouldStartProcessAndReturnInstanceId() {
        DocProcessController controller = new DocProcessController(processApplicationService, processQueryApplicationService);
        when(processApplicationService.start(5L)).thenReturn(999L);

        R<Long> result = controller.start(5L);

        assertEquals(R.SUCCESS, result.getCode());
        assertEquals(999L, result.getData());
        verify(processApplicationService).start(5L);
    }

    @Test
    void shouldReturnProcessConfigByProjectId() {
        DocProcessController controller = new DocProcessController(processApplicationService, processQueryApplicationService);
        DocProcessConfigVo config = new DocProcessConfigVo();
        config.setId(1L);
        config.setProjectId(10L);
        config.setDefinitionId(100L);
        when(processQueryApplicationService.getConfig(10L)).thenReturn(config);

        R<DocProcessConfigVo> result = controller.getConfig(10L);

        assertEquals(R.SUCCESS, result.getCode());
        assertEquals(config, result.getData());
    }

    @Test
    void shouldReturnProcessDefinitionList() {
        DocProcessController controller = new DocProcessController(processApplicationService, processQueryApplicationService);
        List<Map<String, Object>> definitions = List.of(
            Map.of("id", (Object) 1L, "name", (Object) "审批流程"),
            Map.of("id", (Object) 2L, "name", (Object) "签核流程")
        );
        when(processApplicationService.listDefinitions()).thenReturn(definitions);

        R<List<Map<String, Object>>> result = controller.listDefinitions();

        assertEquals(R.SUCCESS, result.getCode());
        assertEquals(2, result.getData().size());
        assertEquals(definitions, result.getData());
    }
}