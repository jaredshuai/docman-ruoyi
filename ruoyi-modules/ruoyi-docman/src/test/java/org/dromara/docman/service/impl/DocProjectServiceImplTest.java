package org.dromara.docman.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.docman.application.assembler.DocProjectAssembler;
import org.dromara.docman.application.port.out.DocumentStoragePort;
import org.dromara.docman.domain.bo.DocProjectBo;
import org.dromara.docman.domain.entity.DocProject;
import org.dromara.docman.domain.entity.DocProjectMember;
import org.dromara.docman.domain.enums.DocNasDirStatus;
import org.dromara.docman.domain.enums.DocProjectRole;
import org.dromara.docman.domain.enums.DocProjectStatus;
import org.dromara.docman.domain.service.DocPathResolver;
import org.dromara.docman.domain.vo.DocProjectVo;
import org.dromara.docman.mapper.DocProjectMapper;
import org.dromara.docman.mapper.DocProjectMemberMapper;
import org.dromara.docman.service.IDocProjectAccessService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("dev")
@Tag("prod")
@Tag("local")
class DocProjectServiceImplTest {

    @BeforeAll
    static void initTableInfo() {
        initTableInfo(DocProject.class);
        initTableInfo(DocProjectMember.class);
    }

    @Mock
    private DocProjectMapper projectMapper;

    @Mock
    private DocProjectMemberMapper memberMapper;

    @Mock
    private DocumentStoragePort documentStoragePort;

    @Mock
    private DocPathResolver docPathResolver;

    @Mock
    private IDocProjectAccessService projectAccessService;

    @Mock
    private DocProjectAssembler projectAssembler;

    @InjectMocks
    private DocProjectServiceImpl service;

    @Test
    void shouldReturnEmptyPageWhenRegularUserHasNoAccessibleProjects() {
        try (MockedStatic<LoginHelper> loginHelper = mockStatic(LoginHelper.class)) {
            loginHelper.when(LoginHelper::isSuperAdmin).thenReturn(false);
            loginHelper.when(LoginHelper::getUserId).thenReturn(66L);
            when(projectMapper.selectAccessibleProjectVoPage(any(Page.class), eq(66L), eq(false), any(LambdaQueryWrapper.class)))
                .thenReturn(new Page<>(1, 10));

            TableDataInfo<DocProjectVo> result = service.queryPageList(new DocProjectBo(), new PageQuery(10, 1));

            assertEquals(0L, result.getTotal());
            assertTrue(result.getRows().isEmpty());
            verify(projectMapper, never()).selectVoPage(any(Page.class), any(LambdaQueryWrapper.class));
        }
    }

    @Test
    void shouldPopulateProjectDetailsWithMemberIdsAndCurrentRole() {
        DocProjectVo project = new DocProjectVo();
        project.setId(12L);
        when(projectMapper.selectVoById(12L)).thenReturn(project);
        when(memberMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(member(12L, 101L, DocProjectRole.OWNER), member(12L, 202L, DocProjectRole.EDITOR)));
        when(projectAccessService.getCurrentRole(12L)).thenReturn(DocProjectRole.EDITOR);

        DocProjectVo result = service.queryById(12L);

        assertEquals(List.of(101L, 202L), result.getMemberIds());
        assertEquals(DocProjectRole.EDITOR.getCode(), result.getCurrentUserRole());
        verify(projectAccessService).assertAction(12L, org.dromara.docman.domain.enums.DocProjectAction.VIEW_PROJECT);
    }

    @Test
    void shouldReturnMyProjectsWithCurrentUserRole() {
        try (MockedStatic<LoginHelper> loginHelper = mockStatic(LoginHelper.class)) {
            loginHelper.when(LoginHelper::isSuperAdmin).thenReturn(false);
            loginHelper.when(LoginHelper::getUserId).thenReturn(66L);
            when(memberMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(member(12L, 66L, DocProjectRole.EDITOR)));
            DocProjectVo project = new DocProjectVo();
            project.setId(12L);
            project.setName("我的项目");
            when(projectMapper.selectVoList(any(LambdaQueryWrapper.class))).thenReturn(List.of(project));

            List<DocProjectVo> result = service.queryMyList(new DocProjectBo());

            assertEquals(1, result.size());
            assertEquals("我的项目", result.get(0).getName());
            assertEquals(DocProjectRole.EDITOR.getCode(), result.get(0).getCurrentUserRole());
        }
    }

    @Test
    void shouldInsertProjectWithCreatedNasDirectoryAndDeduplicatedMembers() {
        DocProjectBo bo = new DocProjectBo();
        bo.setName("Alpha");
        bo.setCustomerType("telecom");
        bo.setOwnerId(100L);
        bo.setMemberIds(List.of(100L, 101L, 102L));

        DocProject mappedProject = new DocProject();
        mappedProject.setName("Alpha");
        mappedProject.setCustomerType("telecom");
        mappedProject.setOwnerId(100L);

        when(projectAssembler.toEntity(bo)).thenReturn(mappedProject);
        when(docPathResolver.buildProjectBasePath("telecom", "Alpha")).thenReturn("/项目文档/2026/电信/Alpha");
        when(documentStoragePort.ensureDirectory("/项目文档/2026/电信/Alpha")).thenReturn(true);
        doAnswer(invocation -> {
            DocProject project = invocation.getArgument(0);
            project.setId(300L);
            return 1;
        }).when(projectMapper).insert(any(DocProject.class));

        Long result = service.insertProject(bo);

        ArgumentCaptor<DocProject> projectCaptor = ArgumentCaptor.forClass(DocProject.class);
        verify(projectMapper).insert(projectCaptor.capture());
        DocProject inserted = projectCaptor.getValue();
        assertEquals(300L, result);
        assertEquals(DocProjectStatus.ACTIVE.getCode(), inserted.getStatus());
        assertEquals("/项目文档/2026/电信/Alpha", inserted.getNasBasePath());
        assertEquals(DocNasDirStatus.CREATED.getCode(), inserted.getNasDirStatus());

        ArgumentCaptor<DocProjectMember> memberCaptor = ArgumentCaptor.forClass(DocProjectMember.class);
        verify(memberMapper, org.mockito.Mockito.times(3)).insert(memberCaptor.capture());
        Map<Long, String> rolesByUserId = memberCaptor.getAllValues().stream()
            .collect(Collectors.toMap(DocProjectMember::getUserId, DocProjectMember::getRoleType));
        assertEquals(3, rolesByUserId.size());
        assertEquals(DocProjectRole.OWNER.getCode(), rolesByUserId.get(100L));
        assertEquals(DocProjectRole.EDITOR.getCode(), rolesByUserId.get(101L));
        assertEquals(DocProjectRole.EDITOR.getCode(), rolesByUserId.get(102L));
        assertTrue(memberCaptor.getAllValues().stream().allMatch(member -> member.getProjectId().equals(300L)));
        assertTrue(memberCaptor.getAllValues().stream().allMatch(member -> member.getCreateTime() != null));
    }

    @Test
    void shouldMarkProjectNasDirectoryPendingWhenDirectoryCreationFails() {
        DocProjectBo bo = new DocProjectBo();
        bo.setName("Beta");
        bo.setCustomerType("social");
        bo.setOwnerId(88L);

        DocProject mappedProject = new DocProject();
        mappedProject.setName("Beta");
        mappedProject.setCustomerType("social");
        mappedProject.setOwnerId(88L);

        when(projectAssembler.toEntity(bo)).thenReturn(mappedProject);
        when(docPathResolver.buildProjectBasePath("social", "Beta")).thenReturn("/项目文档/2026/社会客户/Beta");
        when(documentStoragePort.ensureDirectory("/项目文档/2026/社会客户/Beta")).thenReturn(false);
        doAnswer(invocation -> {
            DocProject project = invocation.getArgument(0);
            project.setId(301L);
            return 1;
        }).when(projectMapper).insert(any(DocProject.class));

        Long result = service.insertProject(bo);

        ArgumentCaptor<DocProject> projectCaptor = ArgumentCaptor.forClass(DocProject.class);
        verify(projectMapper).insert(projectCaptor.capture());
        assertEquals(301L, result);
        assertEquals(DocNasDirStatus.PENDING.getCode(), projectCaptor.getValue().getNasDirStatus());
        verify(memberMapper).insert(any(DocProjectMember.class));
    }

    @Test
    void shouldRetryPendingNasDirectoriesAndOnlyPersistSuccessfulProjects() {
        DocProject first = project(1L, "/nas/p1", DocNasDirStatus.PENDING.getCode());
        DocProject second = project(2L, "/nas/p2", DocNasDirStatus.PENDING.getCode());
        when(projectMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(first, second));
        when(documentStoragePort.ensureDirectory("/nas/p1")).thenReturn(true);
        when(documentStoragePort.ensureDirectory("/nas/p2")).thenReturn(false);

        int result = service.retryPendingNasDirectories();

        assertEquals(1, result);
        assertEquals(DocNasDirStatus.CREATED.getCode(), first.getNasDirStatus());
        assertEquals(DocNasDirStatus.PENDING.getCode(), second.getNasDirStatus());
        ArgumentCaptor<DocProject> updateCaptor = ArgumentCaptor.forClass(DocProject.class);
        verify(projectMapper).updateById(updateCaptor.capture());
        assertEquals(1L, updateCaptor.getValue().getId());
        assertEquals(DocNasDirStatus.CREATED.getCode(), updateCaptor.getValue().getNasDirStatus());
    }

    private DocProjectMember member(Long projectId, Long userId, DocProjectRole role) {
        DocProjectMember member = new DocProjectMember();
        member.setProjectId(projectId);
        member.setUserId(userId);
        member.setRoleType(role.getCode());
        return member;
    }

    private DocProject project(Long id, String nasBasePath, String nasDirStatus) {
        DocProject project = new DocProject();
        project.setId(id);
        project.setNasBasePath(nasBasePath);
        project.setNasDirStatus(nasDirStatus);
        return project;
    }

    private static void initTableInfo(Class<?> entityClass) {
        if (TableInfoHelper.getTableInfo(entityClass) == null) {
            TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), "test"), entityClass);
        }
    }
}
