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
@JobExecutor(name = "documentReminderJob")
@RequiredArgsConstructor
public class DocumentReminderJob {

    private final DocJobApplicationService jobApplicationService;

    public ExecuteResult jobExecute(JobArgs jobArgs) {
        try {
            log.info("开始执行文档缺失提醒任务");
            int remindedCount = jobApplicationService.sendPendingDocumentReminders();
            log.info("文档缺失提醒任务完成，共提醒 {} 个项目", remindedCount);
            return ExecuteResult.success("提醒 " + remindedCount + " 个项目");
        } catch (Exception e) {
            log.error("文档缺失提醒任务执行失败", e);
            return ExecuteResult.failure("执行失败: " + e.getMessage());
        }
    }
}
