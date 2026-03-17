package org.dromara.docman.infrastructure.notify;

import org.dromara.common.core.port.InfrastructureAdapter;
import org.dromara.common.sse.dto.SseMessageDto;
import org.dromara.common.sse.utils.SseMessageUtils;
import org.dromara.docman.application.port.out.SystemMessagePort;

import java.util.List;

@InfrastructureAdapter("系统消息/SSE 通知")
public class SseSystemMessageAdapter implements SystemMessagePort {

    @Override
    public void publishToUsers(List<Long> userIds, String message) {
        if (userIds == null || userIds.isEmpty() || message == null || message.isBlank()) {
            return;
        }
        SseMessageDto dto = new SseMessageDto();
        dto.setUserIds(userIds);
        dto.setMessage(message);
        SseMessageUtils.publishMessage(dto);
    }
}
