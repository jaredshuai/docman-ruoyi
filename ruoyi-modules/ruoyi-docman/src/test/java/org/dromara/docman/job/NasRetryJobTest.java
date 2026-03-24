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
class NasRetryJobTest {

    @Mock
    private DocJobApplicationService jobApplicationService;

    @InjectMocks
    private NasRetryJob nasRetryJob;

    @Test
    void shouldReturnSuccessWhenRetrySucceeds() {
        when(jobApplicationService.retryPendingNasDirectories()).thenReturn(5);

        ExecuteResult result = nasRetryJob.jobExecute(new JobArgs());

        assertEquals(ExecuteResult.success().getStatus(), result.getStatus());
        assertEquals("Task executed successfully", result.getMessage());
        assertEquals("成功处理 5 个项目", result.getResult());
    }

    @Test
    void shouldReturnSuccessWithZeroWhenNoItemsToRetry() {
        when(jobApplicationService.retryPendingNasDirectories()).thenReturn(0);

        ExecuteResult result = nasRetryJob.jobExecute(new JobArgs());

        assertEquals(ExecuteResult.success().getStatus(), result.getStatus());
        assertEquals("Task executed successfully", result.getMessage());
        assertEquals("成功处理 0 个项目", result.getResult());
    }

    @Test
    void shouldReturnFailureWhenExceptionThrown() {
        when(jobApplicationService.retryPendingNasDirectories())
            .thenThrow(new RuntimeException("NAS connection failed"));

        ExecuteResult result = nasRetryJob.jobExecute(new JobArgs());

        assertEquals(ExecuteResult.failure().getStatus(), result.getStatus());
        assertEquals("Task execution failed", result.getMessage());
        assertEquals("执行失败: NAS connection failed", result.getResult());
    }

    @Test
    void shouldReturnFailureWhenNullPointerExceptionThrown() {
        when(jobApplicationService.retryPendingNasDirectories())
            .thenThrow(new NullPointerException("service is null"));

        ExecuteResult result = nasRetryJob.jobExecute(new JobArgs());

        assertEquals(ExecuteResult.failure().getStatus(), result.getStatus());
        assertEquals("Task execution failed", result.getMessage());
        assertEquals("执行失败: service is null", result.getResult());
    }
}
