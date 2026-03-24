package org.dromara.docman.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.domain.event.WorkflowNodeFinishedEvent;
import org.dromara.docman.application.service.DocWorkflowNodeApplicationService;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DocmanNodeListener {

    private final DocWorkflowNodeApplicationService workflowNodeApplicationService;

    @EventListener
    public void onNodeFinish(WorkflowNodeFinishedEvent event) {
        try {
            workflowNodeApplicationService.handleNodeFinished(event);
        } catch (Exception e) {
            log.error("处理节点完成事件失败, event={}", event, e);
        }
    }
}