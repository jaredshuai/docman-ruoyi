package org.dromara.docman.service.impl;

import org.dromara.docman.application.port.out.SystemMessagePort;
import org.dromara.docman.domain.enums.DocProcessConfigStatus;
import org.dromara.docman.service.IDocDocumentRecordService;
import org.dromara.docman.service.IDocProcessConfigService;
import org.dromara.docman.service.IDocProjectService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("dev")
@Tag("prod")
@Tag("local")
class DocDocumentReminderServiceImplTest {

    @Mock
    private IDocProcessConfigService processConfigService;

    @Mock
    private IDocProjectService projectService;

    @Mock
    private IDocDocumentRecordService documentRecordService;

    @Mock
    private SystemMessagePort systemMessagePort;

    @InjectMocks
    private DocDocumentReminderServiceImpl service;

    @Test
    void shouldReturnZeroWhenNoRunningProcessConfigExists() {
        when(processConfigService.listByStatus(DocProcessConfigStatus.RUNNING)).thenReturn(List.of());

        int reminded = service.sendPendingReminders(3);

        assertEquals(0, reminded);
        verify(projectService, never()).listByIdsAndStatus(any(), any());
        verify(systemMessagePort, never()).publishToUsers(anyList(), any());
    }
}
