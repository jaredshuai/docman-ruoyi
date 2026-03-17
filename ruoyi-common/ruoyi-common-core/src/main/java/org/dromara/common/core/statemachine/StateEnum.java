package org.dromara.common.core.statemachine;

/**
 * 状态枚举标记接口。业务枚举实现此接口即可接入 {@link StateMachine} 框架。
 *
 * @param <S> 状态枚举自身类型
 */
public interface StateEnum<S extends Enum<S> & StateEnum<S>> {

    /**
     * 状态编码（持久化用）
     */
    String getCode();

    /**
     * 状态显示名
     */
    String getLabel();
}
