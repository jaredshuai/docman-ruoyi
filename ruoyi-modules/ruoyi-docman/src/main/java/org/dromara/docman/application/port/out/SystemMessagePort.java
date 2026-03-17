package org.dromara.docman.application.port.out;

import org.dromara.common.core.port.OutboundPort;

import java.util.List;

@OutboundPort("系统消息/SSE 通知")
public interface SystemMessagePort {

    void publishToUsers(List<Long> userIds, String message);
}
