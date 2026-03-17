package org.dromara.docman.listener;

import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.event.WorkflowNodeFinishedEvent;
import org.dromara.docman.application.service.DocWorkflowNodeApplicationService;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DocmanNodeListener {

    private final DocWorkflowNodeApplicationService workflowNodeApplicationService;

    @EventListener
    public void onNodeFinish(WorkflowNodeFinishedEvent event) {
        workflowNodeApplicationService.handleNodeFinished(event);
    }
}