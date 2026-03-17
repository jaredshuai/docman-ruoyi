package org.dromara.common.core.permission;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * 通用角色-动作权限策略基类。
 *
 * <p>使用方式：
 * <pre>{@code
 * public class ProjectPermissionPolicy extends RoleActionPolicy<ProjectRole, ProjectAction> {
 *     public ProjectPermissionPolicy() {
 *         super(Map.of(
 *             ProjectRole.OWNER,  EnumSet.allOf(ProjectAction.class),
 *             ProjectRole.EDITOR, EnumSet.of(ProjectAction.VIEW, ProjectAction.EDIT),
 *             ProjectRole.VIEWER, EnumSet.of(ProjectAction.VIEW)
 *         ));
 *     }
 * }
 * }</pre>
 *
 * @param <R> 角色枚举类型
 * @param <A> 动作枚举类型
 */
public abstract class RoleActionPolicy<R extends Enum<R>, A extends Enum<A>> {

    private final Map<R, EnumSet<A>> policy;
    private final Class<A> actionType;

    @SuppressWarnings("unchecked")
    protected RoleActionPolicy(Map<R, EnumSet<A>> policy) {
        this.policy = Collections.unmodifiableMap(policy);
        this.actionType = policy.values().stream()
            .flatMap(Set::stream)
            .findFirst()
            .map(a -> (Class<A>) a.getDeclaringClass())
            .orElse(null);
    }

    /**
     * 角色是否具有指定动作权限。
     */
    public boolean can(R role, A action) {
        if (actionType == null) {
            return false;
        }
        return policy.getOrDefault(role, EnumSet.noneOf(actionType)).contains(action);
    }

    /**
     * 获取角色拥有的所有动作。
     */
    public Set<A> allowedActions(R role) {
        if (actionType == null) {
            return Set.of();
        }
        return Collections.unmodifiableSet(policy.getOrDefault(role, EnumSet.noneOf(actionType)));
    }
}
