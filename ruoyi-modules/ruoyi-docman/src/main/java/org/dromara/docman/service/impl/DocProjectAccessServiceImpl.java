package org.dromara.docman.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.redis.utils.RedisUtils;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.docman.constant.DocmanCacheNames;
import org.dromara.docman.domain.entity.DocProject;
import org.dromara.docman.domain.entity.DocProjectMember;
import org.dromara.docman.domain.enums.DocProjectAction;
import org.dromara.docman.domain.enums.DocProjectRole;
import org.dromara.docman.domain.service.DocProjectPermissionPolicy;
import org.dromara.docman.mapper.DocProjectMapper;
import org.dromara.docman.mapper.DocProjectMemberMapper;
import org.dromara.docman.service.IDocProjectAccessService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DocProjectAccessServiceImpl implements IDocProjectAccessService {

    /**
     * 缓存名称（不含 TTL 后缀，用于构建缓存 key）
     */
    private static final String CACHE_ACCESSIBLE_PROJECTS = "docman_user_accessible_projects";
    private static final String CACHE_PROJECT_ROLE = "docman_user_project_role";

    private final DocProjectMemberMapper memberMapper;
    private final DocProjectMapper projectMapper;
    private final DocProjectPermissionPolicy permissionPolicy;

    @Override
    public List<Long> listAccessibleProjectIds(Long userId) {
        if (LoginHelper.isSuperAdmin(userId)) {
            return projectMapper.selectList(new LambdaQueryWrapper<>()).stream().map(DocProject::getId).toList();
        }
        return memberMapper.selectList(
            new LambdaQueryWrapper<DocProjectMember>()
                .eq(DocProjectMember::getUserId, userId)
                .select(DocProjectMember::getProjectId)
        ).stream().map(DocProjectMember::getProjectId).toList();
    }

    @Override
    public DocProjectRole getCurrentRole(Long projectId) {
        if (LoginHelper.isSuperAdmin()) {
            return DocProjectRole.OWNER;
        }
        DocProjectMember member = memberMapper.selectOne(
            new LambdaQueryWrapper<DocProjectMember>()
                .eq(DocProjectMember::getProjectId, projectId)
                .eq(DocProjectMember::getUserId, LoginHelper.getUserId())
        );
        if (member == null) {
            throw new ServiceException("你无权访问该项目");
        }
        return DocProjectRole.of(member.getRoleType());
    }

    @Override
    public void assertAction(Long projectId, DocProjectAction action) {
        DocProjectRole role = getCurrentRole(projectId);
        if (!permissionPolicy.can(role, action)) {
            throw new ServiceException("当前角色[" + role.getLabel() + "]无权限执行操作: " + action.getCode());
        }
    }

    /**
     * 失效用户可访问项目列表缓存
     *
     * @param userIds 用户ID列表
     */
    @Override
    public void evictAccessibleProjectsCache(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return;
        }
        List<String> keys = new ArrayList<>();
        for (Long userId : userIds) {
            keys.add(CACHE_ACCESSIBLE_PROJECTS + ":" + userId);
        }
        RedisUtils.deleteObject(keys);
    }

    /**
     * 失效用户项目角色缓存
     *
     * @param projectId 项目ID
     * @param userIds   用户ID列表
     */
    @Override
    public void evictProjectRoleCache(Long projectId, List<Long> userIds) {
        if (projectId == null || userIds == null || userIds.isEmpty()) {
            return;
        }
        List<String> keys = new ArrayList<>();
        for (Long userId : userIds) {
            keys.add(CACHE_PROJECT_ROLE + ":" + projectId + ":" + userId);
        }
        RedisUtils.deleteObject(keys);
    }
}
