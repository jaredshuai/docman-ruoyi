package org.dromara.common.core.application;

import java.util.List;

/**
 * 应用层 Assembler 基础接口。负责实体 ↔ VO 的单向或双向转换。
 *
 * <p>实现此接口的 Assembler 应注册为 Spring Bean，由 Application Service 注入使用。
 * 不应在 Controller / Domain Service 层直接调用。
 *
 * @param <E> 实体类型
 * @param <V> 视图对象类型
 */
public interface BaseAssembler<E, V> {

    V toVo(E entity);

    default List<V> toVoList(List<E> entities) {
        if (entities == null) {
            return List.of();
        }
        return entities.stream().map(this::toVo).toList();
    }
}
