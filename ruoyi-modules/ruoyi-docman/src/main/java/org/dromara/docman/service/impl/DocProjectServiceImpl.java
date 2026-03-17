package org.dromara.docman.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.docman.application.port.out.DocumentStoragePort;
import org.dromara.docman.domain.bo.DocProjectBo;
import org.dromara.docman.domain.entity.DocProject;
import org.dromara.docman.domain.entity.DocProjectMember;
import org.dromara.docman.domain.enums.DocProjectAction;
import org.dromara.docman.domain.enums.DocProjectRole;
import org.dromara.docman.domain.service.DocPathResolver;
import org.dromara.docman.domain.vo.DocProjectVo;
import org.dromara.docman.mapper.DocProjectMapper;
import org.dromara.docman.mapper.DocProjectMemberMapper;
import org.dromara.docman.service.IDocProjectAccessService;
import org.dromara.docman.service.IDocProjectService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DocProjectServiceImpl implements IDocProjectService {

    private final DocProjectMapper projectMapper;
    private final DocProjectMemberMapper memberMapper;
    private final DocumentStoragePort documentStoragePort;
    private final DocPathResolver docPathResolver;
    private final IDocProjectAccessService projectAccessService;

    @Override
    public TableDataInfo<DocProjectVo> queryPageList(DocProjectBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<DocProject> wrapper = buildQueryWrapper(bo);
        if (!LoginHelper.isSuperAdmin()) {
            List<Long> projectIds = projectAccessService.listAccessibleProjectIds(LoginHelper.getUserId());

            if (projectIds.isEmpty()) {
                return TableDataInfo.build(new Page<>());
            }
            wrapper.in(DocProject::getId, projectIds);
        }
        Page<DocProjectVo> page = projectMapper.selectVoPage(pageQuery.build(), wrapper);
        return TableDataInfo.build(page);
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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long insertProject(DocProjectBo bo) {
        DocProject project = MapstructUtils.convert(bo, DocProject.class);
        project.setStatus("active");

        String basePath = docPathResolver.buildProjectBasePath(project.getCustomerType(), project.getName());
        project.setNasBasePath(basePath);

        boolean nasOk = documentStoragePort.ensureDirectory(basePath);
        project.setNasDirStatus(nasOk ? "created" : "pending");

        projectMapper.insert(project);

        addMember(project.getId(), bo.getOwnerId(), DocProjectRole.OWNER.getCode());
        if (bo.getMemberIds() != null) {
            for (Long memberId : bo.getMemberIds()) {
                if (!memberId.equals(bo.getOwnerId())) {
                    addMember(project.getId(), memberId, DocProjectRole.EDITOR.getCode());
                }
            }
        }
        return project.getId();
    }

    @Override
    public Boolean updateProject(DocProjectBo bo) {
        projectAccessService.assertAction(bo.getId(), DocProjectAction.EDIT_PROJECT);
        DocProject project = MapstructUtils.convert(bo, DocProject.class);
        return projectMapper.updateById(project) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteByIds(List<Long> ids) {
        ids.forEach(id -> projectAccessService.assertAction(id, DocProjectAction.DELETE_PROJECT));
        memberMapper.delete(
            new LambdaQueryWrapper<DocProjectMember>()
                .in(DocProjectMember::getProjectId, ids)
        );
        return projectMapper.deleteByIds(ids) > 0;
    }

    @Override
    public void retryPendingNasDirectories() {
        List<DocProject> pending = projectMapper.selectList(
            new LambdaQueryWrapper<DocProject>()
                .eq(DocProject::getNasDirStatus, "pending")
        );
        for (DocProject p : pending) {
            boolean ok = documentStoragePort.ensureDirectory(p.getNasBasePath());
            if (ok) {
                p.setNasDirStatus("created");
                projectMapper.updateById(p);
            }
        }
    }

    @Override
    public void assertViewPermission(Long projectId) {
        projectAccessService.assertAction(projectId, DocProjectAction.VIEW_PROJECT);
    }

    private void addMember(Long projectId, Long userId, String roleType) {
        DocProjectMember member = new DocProjectMember();
        member.setProjectId(projectId);
        member.setUserId(userId);
        member.setRoleType(roleType);
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
