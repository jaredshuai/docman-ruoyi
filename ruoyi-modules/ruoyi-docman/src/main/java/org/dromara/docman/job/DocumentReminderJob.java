package org.dromara.docman.job;

import com.aizuda.snailjob.client.job.core.annotation.JobExecutor;
import com.aizuda.snailjob.client.job.core.dto.JobArgs;
import com.aizuda.snailjob.model.dto.ExecuteResult;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.docman.domain.entity.DocDocumentRecord;
import org.dromara.docman.domain.entity.DocProcessConfig;
import org.dromara.docman.domain.entity.DocProject;
import org.dromara.docman.mapper.DocDocumentRecordMapper;
import org.dromara.docman.mapper.DocProcessConfigMapper;
import org.dromara.docman.mapper.DocProjectMapper;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@JobExecutor(name = "documentReminderJob")
@RequiredArgsConstructor
public class DocumentReminderJob {

    private final DocProjectMapper projectMapper;
    private final DocProcessConfigMapper processConfigMapper;
    private final DocDocumentRecordMapper documentRecordMapper;

    public ExecuteResult jobExecute(JobArgs jobArgs) {
        log.info("开始执行文档缺失提醒任务");

        List<DocProcessConfig> runningConfigs = processConfigMapper.selectList(
            new LambdaQueryWrapper<DocProcessConfig>()
                .eq(DocProcessConfig::getStatus, "running")
        );

        int reminded = 0;
        for (DocProcessConfig config : runningConfigs) {
            try {
                DocProject project = projectMapper.selectById(config.getProjectId());
                if (project == null) continue;

                // TODO: 查询 Warm-Flow 当前待办节点，解析 ext 中配置的插件数量
                // TODO: 对比已生成文档数量，若不足则通过 sys_notice 通知负责人
                // 当前为骨架，具体逻辑需确认 Warm-Flow taskService API 后实现
                reminded++;
            } catch (Exception e) {
                log.error("处理项目提醒失败: projectId={}", config.getProjectId(), e);
            }
        }

        log.info("文档缺失提醒任务完成，共处理 {} 个项目", reminded);
        return ExecuteResult.success("处理 " + reminded + " 个项目");
    }
}
