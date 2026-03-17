package org.dromara.common.core.application;

/**
 * 命令型应用服务标记接口。
 *
 * <p>标记在负责写操作（新增/修改/删除/状态变更）的应用服务上，
 * 与 {@link QueryApplicationService} 形成 CQRS 风格约定。
 */
public interface CommandApplicationService {
}
