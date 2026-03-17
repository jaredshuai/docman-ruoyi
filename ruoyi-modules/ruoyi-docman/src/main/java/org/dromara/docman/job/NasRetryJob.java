package org.dromara.docman.job;

import com.aizuda.snailjob.client.job.core.annotation.JobExecutor;
import com.aizuda.snailjob.client.job.core.dto.JobArgs;
import com.aizuda.snailjob.model.dto.ExecuteResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.docman.application.service.DocJobApplicationService;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@JobExecutor(name = "nasRetryJob")
@RequiredArgsConstructor
public class NasRetryJob {

    private final DocJobApplicationService jobApplicationService;

    public ExecuteResult jobExecute(JobArgs jobArgs) {
        log.info("开始执行NAS目录重试任务");
        int retriedCount = jobApplicationService.retryPendingNasDirectories();
        log.info("NAS目录重试任务完成，成功处理 {} 个项目", retriedCount);
        return ExecuteResult.success("成功处理 " + retriedCount + " 个项目");
    }
}
