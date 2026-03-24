package org.dromara.docman.application.service;

import org.dromara.docman.application.assembler.DocProcessAssembler;
import org.dromara.docman.domain.entity.DocProcessConfig;
import org.dromara.docman.domain.vo.DocProcessConfigVo;
import org.dromara.docman.service.IDocProcessService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("dev")
@Tag("prod")
@Tag("local")
class DocProcessQueryApplicationServiceTest {

    @Mock
    private IDocProcessService processService;

    @Mock
    private DocProcessAssembler processAssembler;

    @InjectMocks
    private DocProcessQueryApplicationService queryService;

    @Test
    void shouldDelegateGetConfigToProcessServiceAndAssembler() {
        Long projectId = 1L;
        DocProcessConfig config = new DocProcessConfig();
        DocProcessConfigVo expectedVo = new DocProcessConfigVo();

        when(processService.getByProjectId(projectId)).thenReturn(config);
        when(processAssembler.toVo(config)).thenReturn(expectedVo);

        DocProcessConfigVo result = queryService.getConfig(projectId);

        assertSame(expectedVo, result);
        verify(processService).getByProjectId(projectId);
        verify(processAssembler).toVo(config);
    }

    @Test
    void shouldMapEntityToVoThroughAssembler() {
        Long projectId = 42L;
        DocProcessConfig config = new DocProcessConfig();
        DocProcessConfigVo vo = new DocProcessConfigVo();

        when(processService.getByProjectId(projectId)).thenReturn(config);
        when(processAssembler.toVo(config)).thenReturn(vo);

        DocProcessConfigVo result = queryService.getConfig(projectId);

        assertSame(vo, result, "Should return the VO from assembler");
    }

    @Test
    void shouldReturnNullWhenProcessConfigNotFound() {
        Long projectId = 999L;

        when(processService.getByProjectId(projectId)).thenReturn(null);
        when(processAssembler.toVo(null)).thenReturn(null);

        DocProcessConfigVo result = queryService.getConfig(projectId);

        assertSame(null, result);
        verify(processService).getByProjectId(projectId);
        verify(processAssembler).toVo(null);
    }
}