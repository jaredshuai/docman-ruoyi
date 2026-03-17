package org.dromara.common.core.application;

/**
 * 查询型应用服务标记接口。
 *
 * <p>标记在只负责读取/查询的应用服务上，与 {@link CommandApplicationService} 形成 CQRS 风格约定。
 * <p>实现此接口的类应只调用查询侧的领域/基础设施方法，不产生副作用。
 */
public interface QueryApplicationService {
}
