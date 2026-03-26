package org.dromara.docman.service;

import org.dromara.docman.domain.enums.DocProjectAction;
import org.dromara.docman.domain.enums.DocProjectRole;

import java.util.List;

public interface IDocProjectAccessService {

    List<Long> listAccessibleProjectIds(Long userId);

    DocProjectRole getCurrentRole(Long projectId);

    void assertAction(Long projectId, DocProjectAction action);

    /**
     * 失效用户可访问项目列表缓存
     *
     * @param userIds 用户ID列表
     */
    void evictAccessibleProjectsCache(List<Long> userIds);

    /**
     * 失效用户项目角色缓存
     *
     * @param projectId 项目ID
     * @param userIds   用户ID列表
     */
    void evictProjectRoleCache(Long projectId, List<Long> userIds);
}
