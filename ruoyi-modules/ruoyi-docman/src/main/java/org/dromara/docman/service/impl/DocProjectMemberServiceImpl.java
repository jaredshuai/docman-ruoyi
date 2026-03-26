package org.dromara.docman.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.docman.constant.DocmanCacheNames;
import org.dromara.docman.domain.bo.DocProjectMemberBo;
import org.dromara.docman.domain.entity.DocProject;
import org.dromara.docman.domain.entity.DocProjectMember;
import org.dromara.docman.domain.enums.DocProjectAction;
import org.dromara.docman.domain.enums.DocProjectRole;
import org.dromara.docman.mapper.DocProjectMapper;
import org.dromara.docman.mapper.DocProjectMemberMapper;
import org.dromara.docman.service.IDocProjectAccessService;
import org.dromara.docman.service.IDocProjectMemberService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class DocProjectMemberServiceImpl implements IDocProjectMemberService {

    private static final Set<String> MANAGEABLE_ROLES = Set.of(
        DocProjectRole.EDITOR.getCode(),
        DocProjectRole.VIEWER.getCode()
    );

    private final DocProjectMemberMapper memberMapper;
    private final DocProjectMapper projectMapper;
    private final IDocProjectAccessService projectAccessService;

    @Override
    public List<DocProjectMember> listByProjectId(Long projectId) {
        projectAccessService.assertAction(projectId, DocProjectAction.VIEW_PROJECT);
        return memberMapper.selectList(
            new LambdaQueryWrapper<DocProjectMember>()
                .eq(DocProjectMember::getProjectId, projectId)
                .orderByAsc(DocProjectMember::getCreateTime, DocProjectMember::getId)
        );
    }

    /**
     * 添加项目成员
     * <p>
     * 失效用户可访问项目列表缓存，使新成员立即可以访问项目
     */
    @CacheEvict(cacheNames = DocmanCacheNames.USER_ACCESSIBLE_PROJECTS, key = "#bo.userId")
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long addMember(DocProjectMemberBo bo) {
        Long projectId = bo.getProjectId();
        projectAccessService.assertAction(projectId, DocProjectAction.EDIT_PROJECT);
        DocProject project = getProjectOrThrow(projectId);
        validateRoleType(bo.getRoleType());
        if (project.getOwnerId().equals(bo.getUserId())) {
            throw new ServiceException("项目负责人已是固定成员，无需重复添加");
        }

        DocProjectMember existed = memberMapper.selectOne(
            new LambdaQueryWrapper<DocProjectMember>()
                .eq(DocProjectMember::getProjectId, projectId)
                .eq(DocProjectMember::getUserId, bo.getUserId())
        );
        if (existed != null) {
            throw new ServiceException("项目成员已存在");
        }

        DocProjectMember member = new DocProjectMember();
        member.setProjectId(projectId);
        member.setUserId(bo.getUserId());
        member.setRoleType(bo.getRoleType());
        member.setCreateTime(new Date());
        memberMapper.insert(member);
        return member.getId();
    }

    /**
     * 移除项目成员
     * <p>
     * 失效用户可访问项目列表缓存和用户项目角色缓存，
     * 确保被移除的成员立即失去访问权限
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeMember(Long projectId, Long userId) {
        projectAccessService.assertAction(projectId, DocProjectAction.EDIT_PROJECT);
        DocProject project = getProjectOrThrow(projectId);
        if (project.getOwnerId().equals(userId)) {
            throw new ServiceException("项目负责人不可移除");
        }

        int deleted = memberMapper.delete(
            new LambdaQueryWrapper<DocProjectMember>()
                .eq(DocProjectMember::getProjectId, projectId)
                .eq(DocProjectMember::getUserId, userId)
        );
        if (deleted == 0) {
            throw new ServiceException("项目成员不存在");
        }

        // 失效缓存：用户可访问项目列表 + 用户项目角色
        projectAccessService.evictAccessibleProjectsCache(List.of(userId));
        projectAccessService.evictProjectRoleCache(projectId, List.of(userId));
    }

    private DocProject getProjectOrThrow(Long projectId) {
        DocProject project = projectMapper.selectById(projectId);
        if (project == null) {
            throw new ServiceException("项目不存在");
        }
        return project;
    }

    private void validateRoleType(String roleType) {
        if (!MANAGEABLE_ROLES.contains(roleType)) {
            throw new ServiceException("角色类型非法，仅支持 editor/viewer");
        }
    }
}
