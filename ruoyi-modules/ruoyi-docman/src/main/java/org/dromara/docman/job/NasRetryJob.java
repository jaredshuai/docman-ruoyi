package org.dromara.docman.job;

import com.aizuda.snailjob.client.job.core.annotation.JobExecutor;
import com.aizuda.snailjob.client.job.core.dto.JobArgs;
import com.aizuda.snailjob.model.dto.ExecuteResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.docman.service.IDocProjectService;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@JobExecutor(name = "nasRetryJob")
@RequiredArgsConstructor
public class NasRetryJob {

    private final IDocProjectService projectService;

    public ExecuteResult jobExecute(JobArgs jobArgs) {
        log.info("开始执行NAS目录重试任务");
        projectService.retryPendingNasDirectories();
        log.info("NAS目录重试任务完成");
        return ExecuteResult.success();
    }
}
