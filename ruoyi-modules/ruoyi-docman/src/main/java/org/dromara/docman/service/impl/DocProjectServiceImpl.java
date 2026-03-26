package org.dromara.docman.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.docman.application.assembler.DocProjectAssembler;
import org.dromara.docman.application.port.out.DocumentStoragePort;
import org.dromara.docman.domain.bo.DocProjectBo;
import org.dromara.docman.domain.entity.DocProject;
import org.dromara.docman.domain.entity.DocProjectMember;
import org.dromara.docman.domain.enums.DocNasDirStatus;
import org.dromara.docman.domain.enums.DocProjectAction;
import org.dromara.docman.domain.enums.DocProjectRole;
import org.dromara.docman.domain.enums.DocProjectStatus;
import org.dromara.docman.domain.service.DocPathResolver;
import org.dromara.docman.domain.vo.DocProjectVo;
import org.dromara.docman.mapper.DocProjectMapper;
import org.dromara.docman.mapper.DocProjectMemberMapper;
import org.dromara.docman.service.IDocProjectAccessService;
import org.dromara.docman.service.IDocProjectService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DocProjectServiceImpl implements IDocProjectService {

    private final DocProjectMapper projectMapper;
    private final DocProjectMemberMapper memberMapper;
    private final DocumentStoragePort documentStoragePort;
    private final DocPathResolver docPathResolver;
    private final IDocProjectAccessService projectAccessService;
    private final DocProjectAssembler projectAssembler;

    @Override
    public TableDataInfo<DocProjectVo> queryPageList(DocProjectBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<DocProject> wrapper = buildQueryWrapper(bo);
        boolean isSuperAdmin = LoginHelper.isSuperAdmin();
        Long userId = LoginHelper.getUserId();
        // 使用JOIN查询消除N+1问题
        Page<DocProjectVo> page = projectMapper.selectAccessibleProjectVoPage(
            pageQuery.build(), userId, isSuperAdmin, wrapper);
        return TableDataInfo.build(page);
    }

    @Override
    public List<DocProjectVo> queryMyList(DocProjectBo bo) {
        if (LoginHelper.isSuperAdmin()) {
            List<DocProjectVo> projects = projectMapper.selectVoList(buildQueryWrapper(bo));
            projects.forEach(project -> project.setCurrentUserRole(DocProjectRole.OWNER.getCode()));
            return projects;
        }

        List<DocProjectMember> memberships = memberMapper.selectList(
            new LambdaQueryWrapper<DocProjectMember>()
                .eq(DocProjectMember::getUserId, LoginHelper.getUserId())
                .select(DocProjectMember::getProjectId, DocProjectMember::getRoleType)
        );
        if (memberships.isEmpty()) {
            return List.of();
        }

        Map<Long, String> rolesByProjectId = memberships.stream()
            .collect(Collectors.toMap(DocProjectMember::getProjectId, DocProjectMember::getRoleType, (left, right) -> left));

        LambdaQueryWrapper<DocProject> wrapper = buildQueryWrapper(bo);
        wrapper.in(DocProject::getId, rolesByProjectId.keySet());
        List<DocProjectVo> projects = projectMapper.selectVoList(wrapper);
        projects.forEach(project -> project.setCurrentUserRole(rolesByProjectId.get(project.getId())));
        return projects;
    }

    @Override
    public DocProjectVo queryById(Long id) {
        projectAccessService.assertAction(id, DocProjectAction.VIEW_PROJECT);
        DocProjectVo vo = projectMapper.selectVoById(id);
        if (vo != null) {
            List<Long> memberIds = memberMapper.selectList(
                new LambdaQueryWrapper<DocProjectMember>()
                    .eq(DocProjectMember::getProjectId, id)
                    .select(DocProjectMember::getUserId)
            ).stream().map(DocProjectMember::getUserId).toList();
            vo.setMemberIds(memberIds);
            vo.setCurrentUserRole(projectAccessService.getCurrentRole(id).getCode());
        }
        return vo;
    }

    /**
     * 创建项目
     * <p>
     * 创建项目后会失效所有成员的缓存，确保成员立即可见项目
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long insertProject(DocProjectBo bo) {
        DocProject project = projectAssembler.toEntity(bo);
        project.setStatus(DocProjectStatus.ACTIVE.getCode());

        String basePath = docPathResolver.buildProjectBasePath(project.getCustomerType(), project.getName());
        project.setNasBasePath(basePath);

        boolean nasOk = documentStoragePort.ensureDirectory(basePath);
        project.setNasDirStatus(nasOk ? DocNasDirStatus.CREATED.getCode() : DocNasDirStatus.PENDING.getCode());

        projectMapper.insert(project);

        // 收集受影响的用户ID
        Set<Long> affectedUserIds = new HashSet<>();
        affectedUserIds.add(bo.getOwnerId());

        addMember(project.getId(), bo.getOwnerId(), DocProjectRole.OWNER.getCode());
        if (bo.getMemberIds() != null) {
            for (Long memberId : bo.getMemberIds()) {
                if (!memberId.equals(bo.getOwnerId())) {
                    addMember(project.getId(), memberId, DocProjectRole.EDITOR.getCode());
                    affectedUserIds.add(memberId);
                }
            }
        }

        // 失效所有受影响用户的缓存
        projectAccessService.evictAccessibleProjectsCache(new ArrayList<>(affectedUserIds));

        return project.getId();
    }

    @Override
    public Boolean updateProject(DocProjectBo bo) {
        projectAccessService.assertAction(bo.getId(), DocProjectAction.EDIT_PROJECT);
        DocProject project = projectAssembler.toEntity(bo);
        return projectMapper.updateById(project) > 0;
    }

    /**
     * 删除项目
     * <p>
     * 删除前先查询受影响的成员，删除后失效缓存
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteByIds(List<Long> ids) {
        ids.forEach(id -> projectAccessService.assertAction(id, DocProjectAction.DELETE_PROJECT));

        // 先查询受影响的成员
        List<DocProjectMember> affectedMembers = memberMapper.selectList(
            new LambdaQueryWrapper<DocProjectMember>()
                .in(DocProjectMember::getProjectId, ids)
        );

        // 按项目分组用户ID
        java.util.Map<Long, List<Long>> projectUserMap = affectedMembers.stream()
            .collect(Collectors.groupingBy(
                DocProjectMember::getProjectId,
                Collectors.mapping(DocProjectMember::getUserId, Collectors.toList())
            ));

        // 删除成员和项目
        memberMapper.delete(
            new LambdaQueryWrapper<DocProjectMember>()
                .in(DocProjectMember::getProjectId, ids)
        );
        boolean result = projectMapper.deleteByIds(ids) > 0;

        // 批量失效缓存
        Set<Long> allUserIds = affectedMembers.stream()
            .map(DocProjectMember::getUserId)
            .collect(Collectors.toSet());
        projectAccessService.evictAccessibleProjectsCache(new ArrayList<>(allUserIds));

        // 失效用户项目角色缓存
        projectUserMap.forEach((projectId, userIds) ->
            projectAccessService.evictProjectRoleCache(projectId, userIds)
        );

        return result;
    }

    @Override
    public int retryPendingNasDirectories() {
        List<DocProject> pending = projectMapper.selectList(
            new LambdaQueryWrapper<DocProject>()
                .eq(DocProject::getNasDirStatus, DocNasDirStatus.PENDING.getCode())
        );
        int successCount = 0;
        for (DocProject p : pending) {
            boolean ok = documentStoragePort.ensureDirectory(p.getNasBasePath());
            if (ok) {
                p.setNasDirStatus(DocNasDirStatus.CREATED.getCode());
                projectMapper.updateById(p);
                successCount++;
            }
        }
        return successCount;
    }

    @Override
    public void assertViewPermission(Long projectId) {
        projectAccessService.assertAction(projectId, DocProjectAction.VIEW_PROJECT);
    }

    @Override
    public List<DocProject> listByIdsAndStatus(Collection<Long> ids, DocProjectStatus status) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return projectMapper.selectList(
            new LambdaQueryWrapper<DocProject>()
                .in(DocProject::getId, ids)
                .eq(DocProject::getStatus, status.getCode())
        );
    }

    private void addMember(Long projectId, Long userId, String roleType) {
        DocProjectMember member = new DocProjectMember();
        member.setProjectId(projectId);
        member.setUserId(userId);
        member.setRoleType(roleType);
        member.setCreateTime(new java.util.Date());
        memberMapper.insert(member);
    }

    private LambdaQueryWrapper<DocProject> buildQueryWrapper(DocProjectBo bo) {
        LambdaQueryWrapper<DocProject> lqw = Wrappers.lambdaQuery();
        lqw.like(StringUtils.isNotBlank(bo.getName()), DocProject::getName, bo.getName());
        lqw.eq(StringUtils.isNotBlank(bo.getCustomerType()), DocProject::getCustomerType, bo.getCustomerType());
        lqw.eq(StringUtils.isNotBlank(bo.getBusinessType()), DocProject::getBusinessType, bo.getBusinessType());
        lqw.orderByDesc(DocProject::getCreateTime);
        return lqw;
    }
}
