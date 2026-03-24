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
class DocumentReminderJobTest {

    @Mock
    private DocJobApplicationService jobApplicationService;

    @InjectMocks
    private DocumentReminderJob documentReminderJob;

    @Test
    void shouldReturnSuccessWhenRemindersSentSuccessfully() {
        when(jobApplicationService.sendPendingDocumentReminders()).thenReturn(5);

        ExecuteResult result = documentReminderJob.jobExecute(new JobArgs());

        assertEquals(ExecuteResult.success().getStatus(), result.getStatus());
        assertEquals("Task executed successfully", result.getMessage());
        assertEquals("提醒 5 个项目", result.getResult());
    }

    @Test
    void shouldReturnSuccessWithZeroWhenNoRemindersNeeded() {
        when(jobApplicationService.sendPendingDocumentReminders()).thenReturn(0);

        ExecuteResult result = documentReminderJob.jobExecute(new JobArgs());

        assertEquals(ExecuteResult.success().getStatus(), result.getStatus());
        assertEquals("Task executed successfully", result.getMessage());
        assertEquals("提醒 0 个项目", result.getResult());
    }

    @Test
    void shouldReturnFailureWhenExceptionThrown() {
        when(jobApplicationService.sendPendingDocumentReminders())
            .thenThrow(new RuntimeException("数据库连接失败"));

        ExecuteResult result = documentReminderJob.jobExecute(new JobArgs());

        assertEquals(ExecuteResult.failure().getStatus(), result.getStatus());
        assertEquals("Task execution failed", result.getMessage());
        assertEquals("执行失败: 数据库连接失败", result.getResult());
    }

    @Test
    void shouldReturnFailureWhenNullPointerExceptionThrown() {
        when(jobApplicationService.sendPendingDocumentReminders())
            .thenThrow(new NullPointerException("服务为空"));

        ExecuteResult result = documentReminderJob.jobExecute(new JobArgs());

        assertEquals(ExecuteResult.failure().getStatus(), result.getStatus());
        assertEquals("Task execution failed", result.getMessage());
        assertEquals("执行失败: 服务为空", result.getResult());
    }
}
