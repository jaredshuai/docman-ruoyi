package org.dromara.docman.listener;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * 节点完成事件 - 由 WorkflowGlobalListener.finish() 发布
 *
 * @deprecated 已迁移为 org.dromara.common.core.domain.event.WorkflowNodeFinishedEvent，
 * 请勿再新增依赖到 docman 模块内部事件。
 */
@Deprecated(forRemoval = true)
@Getter
public class DocmanNodeFinishEvent extends ApplicationEvent {

    private final Long processInstanceId;
    private final String nodeCode;
    private final String nodeExt;

    public DocmanNodeFinishEvent(Object source, Long processInstanceId,
                                  String nodeCode, String nodeExt) {
        super(source);
        this.processInstanceId = processInstanceId;
        this.nodeCode = nodeCode;
        this.nodeExt = nodeExt;
    }
}
