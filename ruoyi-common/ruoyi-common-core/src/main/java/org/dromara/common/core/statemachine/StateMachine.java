package org.dromara.common.core.statemachine;

import org.dromara.common.core.exception.ServiceException;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * 通用有限状态机。基于预定义的状态转移表验证状态迁移合法性。
 *
 * <p>使用方式：
 * <pre>{@code
 * public class OrderStateMachine extends StateMachine<OrderStatus> {
 *     public OrderStateMachine() {
 *         super(Map.of(
 *             OrderStatus.CREATED,   EnumSet.of(OrderStatus.PAID, OrderStatus.CANCELLED),
 *             OrderStatus.PAID,      EnumSet.of(OrderStatus.SHIPPED, OrderStatus.REFUNDED),
 *             OrderStatus.SHIPPED,   EnumSet.of(OrderStatus.COMPLETED),
 *             OrderStatus.COMPLETED, EnumSet.noneOf(OrderStatus.class),
 *             OrderStatus.CANCELLED, EnumSet.noneOf(OrderStatus.class),
 *             OrderStatus.REFUNDED,  EnumSet.noneOf(OrderStatus.class)
 *         ));
 *     }
 * }
 * }</pre>
 *
 * @param <S> 状态枚举类型，须实现 {@link StateEnum}
 */
public abstract class StateMachine<S extends Enum<S> & StateEnum<S>> {

    private final Map<S, EnumSet<S>> transitions;

    protected StateMachine(Map<S, EnumSet<S>> transitions) {
        this.transitions = Collections.unmodifiableMap(transitions);
    }

    /**
     * 校验状态迁移合法性，非法则抛出 ServiceException。
     */
    public void ensureTransition(S current, S target) {
        if (current == target) {
            return;
        }
        Set<S> allowed = transitions.get(current);
        if (allowed == null || !allowed.contains(target)) {
            throw new ServiceException("非法状态变更: " + current.getCode() + " -> " + target.getCode());
        }
    }

    /**
     * 检查从当前状态能否迁移到目标状态。
     */
    public boolean canTransition(S current, S target) {
        if (current == target) {
            return true;
        }
        Set<S> allowed = transitions.get(current);
        return allowed != null && allowed.contains(target);
    }

    /**
     * 获取当前状态可达的所有目标状态。
     */
    public Set<S> allowedTargets(S current) {
        return transitions.getOrDefault(current, EnumSet.noneOf(current.getDeclaringClass()));
    }

    /**
     * 检查当前状态是否为终态（无出边）。
     */
    public boolean isTerminal(S state) {
        return allowedTargets(state).isEmpty();
    }
}
