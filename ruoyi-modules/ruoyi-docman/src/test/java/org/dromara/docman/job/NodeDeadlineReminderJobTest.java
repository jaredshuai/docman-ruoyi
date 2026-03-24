package org.dromara.docman.job;

import com.aizuda.snailjob.client.job.core.dto.JobArgs;
import com.aizuda.snailjob.model.dto.ExecuteResult;
import org.dromara.docman.application.service.DocJobApplicationService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("dev")
@Tag("prod")
@Tag("local")
class NodeDeadlineReminderJobTest {

    @Mock
    private DocJobApplicationService jobApplicationService;

    @InjectMocks
    private NodeDeadlineReminderJob nodeDeadlineReminderJob;

    @Test
    void shouldReturnSuccessWhenRemindersSentSuccessfully() {
        when(jobApplicationService.sendNodeDeadlineReminders()).thenReturn(4);

        ExecuteResult result = nodeDeadlineReminderJob.jobExecute(new JobArgs());

        assertEquals(ExecuteResult.success().getStatus(), result.getStatus());
        assertEquals("Task executed successfully", result.getMessage());
        assertEquals("提醒 4 条记录", result.getResult());
    }

    @Test
    void shouldReturnFailureWhenExceptionThrown() {
        when(jobApplicationService.sendNodeDeadlineReminders())
            .thenThrow(new RuntimeException("deadline service unavailable"));

        ExecuteResult result = nodeDeadlineReminderJob.jobExecute(new JobArgs());

        assertEquals(ExecuteResult.failure().getStatus(), result.getStatus());
        assertEquals("Task execution failed", result.getMessage());
        assertEquals("执行失败: deadline service unavailable", result.getResult());
    }
}
