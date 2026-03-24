package org.dromara.docman.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.docman.application.port.out.SystemMessagePort;
import org.dromara.docman.domain.bo.DocNodeDeadlineBo;
import org.dromara.docman.domain.entity.DocNodeDeadline;
import org.dromara.docman.domain.enums.DocProjectAction;
import org.dromara.docman.domain.vo.DocNodeDeadlineVo;
import org.dromara.docman.mapper.DocNodeDeadlineMapper;
import org.dromara.docman.service.IDocNodeDeadlineService;
import org.dromara.docman.service.IDocProjectAccessService;
import org.dromara.docman.service.IDocProjectService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocNodeDeadlineServiceImpl implements IDocNodeDeadlineService {

    private final DocNodeDeadlineMapper nodeDeadlineMapper;
    private final IDocProjectAccessService projectAccessService;
    private final IDocProjectService projectService;
    private final SystemMessagePort systemMessagePort;

    @Override
    public void createDeadline(Long processInstanceId, String nodeCode, Long projectId, int durationDays) {
        if (durationDays <= 0) {
            return;
        }
        DocNodeDeadline existing = nodeDeadlineMapper.selectOne(
            new LambdaQueryWrapper<DocNodeDeadline>()
                .eq(DocNodeDeadline::getProcessInstanceId, processInstanceId)
                .eq(DocNodeDeadline::getNodeCode, nodeCode)
        );
        LocalDate deadline = LocalDate.now().plusDays(durationDays);
        if (existing != null) {
            existing.setProjectId(projectId);
            existing.setDurationDays(durationDays);
            existing.setDeadline(deadline);
            existing.setReminderCount(0);
            existing.setLastRemindedAt(null);
            nodeDeadlineMapper.updateById(existing);
            return;
        }

        DocNodeDeadline record = new DocNodeDeadline();
        record.setProcessInstanceId(processInstanceId);
        record.setNodeCode(nodeCode);
        record.setProjectId(projectId);
        record.setDurationDays(durationDays);
        record.setDeadline(deadline);
        record.setReminderCount(0);
        record.setLastRemindedAt(null);
        nodeDeadlineMapper.insert(record);
    }

    @Override
    public List<DocNodeDeadlineVo> listByProject(Long projectId) {
        projectAccessService.assertAction(projectId, DocProjectAction.VIEW_PROCESS);
        return nodeDeadlineMapper.selectDeadlineList(projectId);
    }

    @Override
    public void updateDeadline(DocNodeDeadlineBo bo) {
        DocNodeDeadline record = nodeDeadlineMapper.selectById(bo.getId());
        if (record == null) {
            throw new ServiceException("节点截止记录不存在");
        }
        projectAccessService.assertAction(record.getProjectId(), DocProjectAction.EDIT_PROJECT);
        if (bo.getDeadline() == null && bo.getDurationDays() == null) {
            throw new ServiceException("截止日期和时限天数不能同时为空");
        }
        if (bo.getDurationDays() != null && bo.getDurationDays() <= 0) {
            throw new ServiceException("时限天数必须大于 0");
        }
        if (bo.getDeadline() != null) {
            record.setDeadline(bo.getDeadline());
        }
        if (bo.getDurationDays() != null) {
            record.setDurationDays(bo.getDurationDays());
        }
        if (bo.getDeadline() != null) {
            record.setReminderCount(0);
            record.setLastRemindedAt(null);
        }
        nodeDeadlineMapper.updateById(record);
    }

    @Override
    public int sendApproachingDeadlineReminders(int reminderAdvanceDays, int maxReminderCount) {
        LocalDate deadlineBefore = LocalDate.now().plusDays(reminderAdvanceDays);
        List<DocNodeDeadlineVo> approaching = nodeDeadlineMapper.selectApproachingDeadlines(deadlineBefore, maxReminderCount);
        if (approaching.isEmpty()) {
            return 0;
        }

        int remindedCount = 0;
        for (DocNodeDeadlineVo vo : approaching) {
            try {
                var project = projectService.queryById(vo.getProjectId());
                if (project == null || project.getOwnerId() == null) {
                    log.warn("跳过节点截止提醒，项目或负责人缺失: projectId={}", vo.getProjectId());
                    continue;
                }

                String message = "项目【" + project.getName() + "】节点【" + vo.getNodeCode()
                    + "】文档截止日期为 " + vo.getDeadline() + "，请及时处理";
                systemMessagePort.publishToUsers(List.of(project.getOwnerId()), message);

                DocNodeDeadline record = nodeDeadlineMapper.selectById(vo.getId());
                if (record != null) {
                    record.setReminderCount(record.getReminderCount() + 1);
                    record.setLastRemindedAt(LocalDateTime.now());
                    nodeDeadlineMapper.updateById(record);
                }
                remindedCount++;
            } catch (Exception e) {
                log.error("发送节点截止提醒失败: deadlineId={}", vo.getId(), e);
            }
        }
        return remindedCount;
    }
}
