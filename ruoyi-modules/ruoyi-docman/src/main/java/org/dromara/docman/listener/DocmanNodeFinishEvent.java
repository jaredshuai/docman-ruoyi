package org.dromara.docman.listener;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * 节点完成事件。
 * <p>
 * 设计意图：由 WorkflowGlobalListener.finish() 发布，用于触发节点完成后的业务处理。
 * 计划对接点：文档归档、消息通知、状态同步等监听器。
 * 当前状态：骨架事件类，尚未接入业务流程。
 */
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
