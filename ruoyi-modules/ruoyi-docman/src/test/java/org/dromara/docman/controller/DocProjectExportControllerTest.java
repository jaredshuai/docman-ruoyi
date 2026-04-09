package org.dromara.docman.controller;

import org.dromara.common.core.domain.R;
import org.dromara.docman.application.service.DocProjectExportApplicationService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@Tag("dev")
@Tag("prod")
@Tag("local")
class DocProjectExportControllerTest {

    @Mock
    private DocProjectExportApplicationService applicationService;

    @Test
    void shouldTriggerExportThroughApplicationService() {
        DocProjectExportController controller = new DocProjectExportController(applicationService);

        R<Void> result = controller.trigger(7L);

        assertEquals(R.SUCCESS, result.getCode());
        verify(applicationService).triggerExportText(7L);
    }
}
