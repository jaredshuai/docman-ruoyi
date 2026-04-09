package org.dromara.docman.controller;

import org.dromara.common.core.domain.R;
import org.dromara.docman.application.service.DocProjectBalanceAdjustmentApplicationService;
import org.dromara.docman.application.service.DocProjectBalanceAdjustmentQueryApplicationService;
import org.dromara.docman.domain.bo.DocProjectBalanceAdjustmentBo;
import org.dromara.docman.domain.vo.DocProjectBalanceAdjustmentVo;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("dev")
@Tag("prod")
@Tag("local")
class DocProjectBalanceAdjustmentControllerTest {

    @Mock
    private DocProjectBalanceAdjustmentQueryApplicationService queryApplicationService;

    @Mock
    private DocProjectBalanceAdjustmentApplicationService applicationService;

    @Test
    void shouldReturnLatestBalanceAdjustment() {
        DocProjectBalanceAdjustmentController controller = new DocProjectBalanceAdjustmentController(queryApplicationService, applicationService);
        DocProjectBalanceAdjustmentVo vo = new DocProjectBalanceAdjustmentVo();
        vo.setProjectId(5L);
        when(queryApplicationService.queryLatest(5L)).thenReturn(vo);

        R<DocProjectBalanceAdjustmentVo> result = controller.latest(5L);

        assertEquals(R.SUCCESS, result.getCode());
        assertEquals(vo, result.getData());
    }

    @Test
    void shouldSaveBalanceAdjustmentUnderProjectPath() {
        DocProjectBalanceAdjustmentController controller = new DocProjectBalanceAdjustmentController(queryApplicationService, applicationService);
        DocProjectBalanceAdjustmentBo bo = new DocProjectBalanceAdjustmentBo();
        bo.setMaterialPrice(new BigDecimal("88"));
        when(applicationService.save(bo)).thenReturn(12L);

        R<Long> result = controller.save(9L, bo);

        assertEquals(R.SUCCESS, result.getCode());
        assertEquals(12L, result.getData());
        assertEquals(9L, bo.getProjectId());
        verify(applicationService).save(bo);
    }
}
