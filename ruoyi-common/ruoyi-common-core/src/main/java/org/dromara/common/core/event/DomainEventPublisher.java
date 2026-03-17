package org.dromara.common.core.event;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.dromara.common.core.utils.SpringUtils;

/**
 * 领域事件发布器。
 *
 * <p>提供静态方法发布领域/应用事件，使领域层代码不直接依赖 Spring ApplicationContext。
 *
 * <p>用法：
 * <pre>{@code
 * DomainEventPublisher.publish(new OrderCreatedEvent(orderId));
 * }</pre>
 *
 * <p>事件对象可以是任意类型，订阅侧使用 {@code @EventListener} 或 {@code @TransactionalEventListener}。
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class DomainEventPublisher {

    /**
     * 发布事件到 Spring ApplicationContext。
     *
     * @param event 事件对象
     */
    public static void publish(Object event) {
        SpringUtils.context().publishEvent(event);
    }

    /**
     * 仅在 ApplicationContext 可用时发布（避免启动阶段 NPE）。
     *
     * @param event 事件对象
     * @return 是否成功发布
     */
    public static boolean publishIfReady(Object event) {
        try {
            SpringUtils.context().publishEvent(event);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
