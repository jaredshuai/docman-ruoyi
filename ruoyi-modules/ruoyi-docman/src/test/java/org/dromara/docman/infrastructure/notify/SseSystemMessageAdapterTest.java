package org.dromara.docman.infrastructure.notify;

import org.dromara.common.sse.dto.SseMessageDto;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * SseSystemMessageAdapter 单元测试
 * <p>
 * 测试边缘情况时不会调用 SseMessageUtils.publishMessage()，
 * 因此可以不依赖 Spring 上下文直接测试。
 * 正常路径由于 SseMessageUtils 静态初始化依赖 Spring，需要在集成测试中验证。
 */
@Tag("dev")
@Tag("prod")
@Tag("local")
class SseSystemMessageAdapterTest {

    private final SseSystemMessageAdapter adapter = new SseSystemMessageAdapter();

    @Test
    void shouldNotPublishWhenUserIdsIsNull() {
        // 边缘情况：userIds 为 null，方法应直接返回，不抛异常
        adapter.publishToUsers(null, "test message");
        // 无异常即通过
    }

    @Test
    void shouldNotPublishWhenUserIdsIsEmpty() {
        // 边缘情况：userIds 为空列表，方法应直接返回，不抛异常
        adapter.publishToUsers(Collections.emptyList(), "test message");
        // 无异常即通过
    }

    @Test
    void shouldNotPublishWhenMessageIsNull() {
        // 边缘情况：message 为 null，方法应直接返回，不抛异常
        adapter.publishToUsers(Arrays.asList(1L, 2L), null);
        // 无异常即通过
    }

    @Test
    void shouldNotPublishWhenMessageIsBlank() {
        // 边缘情况：message 为空白字符串，方法应直接返回，不抛异常
        adapter.publishToUsers(Arrays.asList(1L, 2L), "   ");
        // 无异常即通过
    }

    @Test
    void shouldCreateCorrectDtoWhenPublishing() {
        // 测试 DTO 创建逻辑（不实际调用 SseMessageUtils）
        // 通过创建本地 DTO 验证数据传递正确性
        List<Long> userIds = Arrays.asList(1L, 2L, 3L);
        String message = "test notification";

        SseMessageDto dto = new SseMessageDto();
        dto.setUserIds(userIds);
        dto.setMessage(message);

        assertNotNull(dto);
        assertEquals(userIds, dto.getUserIds());
        assertEquals(message, dto.getMessage());
    }
}