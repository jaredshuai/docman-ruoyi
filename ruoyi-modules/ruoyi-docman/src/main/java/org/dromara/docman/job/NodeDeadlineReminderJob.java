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
@JobExecutor(name = "nodeDeadlineReminderJob")
@RequiredArgsConstructor
public class NodeDeadlineReminderJob {

    private final DocJobApplicationService jobApplicationService;

    public ExecuteResult jobExecute(JobArgs jobArgs) {
        try {
            log.info("开始执行节点截止提醒任务");
            int remindedCount = jobApplicationService.sendNodeDeadlineReminders();
            log.info("节点截止提醒任务完成，共提醒 {} 条记录", remindedCount);
            return ExecuteResult.success("提醒 " + remindedCount + " 条记录");
        } catch (Exception e) {
            log.error("节点截止提醒任务执行失败", e);
            return ExecuteResult.failure("执行失败: " + e.getMessage());
        }
    }
}
