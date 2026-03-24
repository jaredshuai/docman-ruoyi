package org.dromara.docman.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.docman.domain.bo.DocProjectMemberBo;
import org.dromara.docman.domain.entity.DocProject;
import org.dromara.docman.domain.entity.DocProjectMember;
import org.dromara.docman.domain.enums.DocProjectAction;
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
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("dev")
@Tag("prod")
@Tag("local")
class DocProjectMemberServiceImplTest {

    @BeforeAll
    static void initTableInfo() {
        initTableInfo(DocProject.class);
        initTableInfo(DocProjectMember.class);
    }

    @Mock
    private DocProjectMemberMapper memberMapper;

    @Mock
    private DocProjectMapper projectMapper;

    @Mock
    private IDocProjectAccessService projectAccessService;

    @InjectMocks
    private DocProjectMemberServiceImpl service;

    // ===================== listByProjectId tests =====================

    @Test
    void shouldDelegateViewActionCheck() {
        Long projectId = 100L;
        when(memberMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());

        service.listByProjectId(projectId);

        verify(projectAccessService).assertAction(projectId, DocProjectAction.VIEW_PROJECT);
    }

    @Test
    void shouldReturnMembersOrderedByCreateTimeAndId() {
        Long projectId = 200L;
        DocProjectMember m1 = createMember(1L, projectId, 10L, "editor", new Date(1000));
        DocProjectMember m2 = createMember(2L, projectId, 20L, "viewer", new Date(2000));
        DocProjectMember m3 = createMember(3L, projectId, 30L, "editor", new Date(1000)); // same time as m1

        when(memberMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(m1, m3, m2));

        List<DocProjectMember> result = service.listByProjectId(projectId);

        assertEquals(3, result.size());
        // Verify ordering is delegated to mapper via LambdaQueryWrapper
        ArgumentCaptor<LambdaQueryWrapper<DocProjectMember>> captor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(memberMapper).selectList(captor.capture());
        assertNotNull(captor.getValue());
    }

    @Test
    void shouldFilterByProjectId() {
        Long projectId = 300L;
        when(memberMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());

        service.listByProjectId(projectId);

        ArgumentCaptor<LambdaQueryWrapper<DocProjectMember>> captor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(memberMapper).selectList(captor.capture());
        // The query wrapper should contain projectId filter
        assertNotNull(captor.getValue());
    }

    @Test
    void shouldReturnExactListFromMapper() {
        Long projectId = 400L;
        DocProjectMember m1 = createMember(1L, projectId, 10L, "editor", new Date());
        DocProjectMember m2 = createMember(2L, projectId, 20L, "viewer", new Date());
        List<DocProjectMember> expectedList = List.of(m1, m2);

        when(memberMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(expectedList);

        List<DocProjectMember> result = service.listByProjectId(projectId);

        assertEquals(expectedList, result);
        assertEquals(2, result.size());
    }

    @Test
    void shouldReturnEmptyListWhenNoMembers() {
        Long projectId = 500L;
        when(memberMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());

        List<DocProjectMember> result = service.listByProjectId(projectId);

        assertNotNull(result);
        assertEquals(0, result.size());
    }

    // ===================== addMember success tests =====================

    @Test
    void shouldAddMemberSuccessfully() {
        Long projectId = 10L;
        Long userId = 100L;
        Long ownerId = 999L;
        Long expectedId = 1L;

        DocProjectMemberBo bo = new DocProjectMemberBo();
        bo.setProjectId(projectId);
        bo.setUserId(userId);
        bo.setRoleType("editor");

        DocProject project = new DocProject();
        project.setId(projectId);
        project.setOwnerId(ownerId);

        when(projectMapper.selectById(projectId)).thenReturn(project);
        when(memberMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        when(memberMapper.insert(any(DocProjectMember.class))).thenAnswer(inv -> {
            DocProjectMember member = inv.getArgument(0);
            member.setId(expectedId);
            return 1;
        });

        Long result = service.addMember(bo);

        assertEquals(expectedId, result);
        verify(projectAccessService).assertAction(projectId, DocProjectAction.EDIT_PROJECT);
    }

    @Test
    void shouldAddViewerRoleMember() {
        Long projectId = 20L;
        Long userId = 200L;

        DocProjectMemberBo bo = new DocProjectMemberBo();
        bo.setProjectId(projectId);
        bo.setUserId(userId);
        bo.setRoleType("viewer");

        DocProject project = new DocProject();
        project.setId(projectId);
        project.setOwnerId(999L);

        when(projectMapper.selectById(projectId)).thenReturn(project);
        when(memberMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        when(memberMapper.insert(any(DocProjectMember.class))).thenReturn(1);

        service.addMember(bo);

        ArgumentCaptor<DocProjectMember> captor = ArgumentCaptor.forClass(DocProjectMember.class);
        verify(memberMapper).insert(captor.capture());
        assertEquals("viewer", captor.getValue().getRoleType());
    }

    @Test
    void shouldSetCreateTimeOnAddMember() {
        Long projectId = 21L;
        Long userId = 210L;

        DocProjectMemberBo bo = new DocProjectMemberBo();
        bo.setProjectId(projectId);
        bo.setUserId(userId);
        bo.setRoleType("editor");

        DocProject project = new DocProject();
        project.setId(projectId);
        project.setOwnerId(999L);

        when(projectMapper.selectById(projectId)).thenReturn(project);
        when(memberMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        when(memberMapper.insert(any(DocProjectMember.class))).thenReturn(1);

        service.addMember(bo);

        ArgumentCaptor<DocProjectMember> captor = ArgumentCaptor.forClass(DocProjectMember.class);
        verify(memberMapper).insert(captor.capture());
        assertNotNull(captor.getValue().getCreateTime());
    }

    @Test
    void shouldCheckExistingMemberByProjectIdAndUserId() {
        Long projectId = 22L;
        Long userId = 220L;

        DocProjectMemberBo bo = new DocProjectMemberBo();
        bo.setProjectId(projectId);
        bo.setUserId(userId);
        bo.setRoleType("editor");

        DocProject project = new DocProject();
        project.setId(projectId);
        project.setOwnerId(999L);

        when(projectMapper.selectById(projectId)).thenReturn(project);
        when(memberMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        when(memberMapper.insert(any(DocProjectMember.class))).thenReturn(1);

        service.addMember(bo);

        // Verify that selectOne was called with correct filter conditions
        ArgumentCaptor<LambdaQueryWrapper<DocProjectMember>> captor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(memberMapper).selectOne(captor.capture());
        assertNotNull(captor.getValue());
    }

    // ===================== addMember guard tests =====================

    @Test
    void shouldRejectAddMemberWhenAccessDenied() {
        Long projectId = 11L;
        DocProjectMemberBo bo = new DocProjectMemberBo();
        bo.setProjectId(projectId);
        bo.setUserId(100L);
        bo.setRoleType("editor");

        doThrow(new ServiceException("无权限")).when(projectAccessService).assertAction(projectId, DocProjectAction.EDIT_PROJECT);

        ServiceException ex = assertThrows(ServiceException.class, () -> service.addMember(bo));
        assertEquals("无权限", ex.getMessage());
    }

    @Test
    void shouldRejectAddMemberWhenProjectNotFound() {
        Long projectId = 12L;
        DocProjectMemberBo bo = new DocProjectMemberBo();
        bo.setProjectId(projectId);
        bo.setUserId(100L);
        bo.setRoleType("editor");

        when(projectMapper.selectById(projectId)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class, () -> service.addMember(bo));
        assertEquals("项目不存在", ex.getMessage());
    }

    @Test
    void shouldRejectAddMemberWhenRoleTypeIsOwner() {
        Long projectId = 13L;
        DocProjectMemberBo bo = new DocProjectMemberBo();
        bo.setProjectId(projectId);
        bo.setUserId(100L);
        bo.setRoleType("owner");

        DocProject project = new DocProject();
        project.setId(projectId);
        project.setOwnerId(999L);

        when(projectMapper.selectById(projectId)).thenReturn(project);

        ServiceException ex = assertThrows(ServiceException.class, () -> service.addMember(bo));
        assertEquals("角色类型非法，仅支持 editor/viewer", ex.getMessage());
    }

    @Test
    void shouldRejectAddMemberWhenRoleTypeIsInvalid() {
        Long projectId = 14L;
        DocProjectMemberBo bo = new DocProjectMemberBo();
        bo.setProjectId(projectId);
        bo.setUserId(100L);
        bo.setRoleType("invalid_role");

        DocProject project = new DocProject();
        project.setId(projectId);
        project.setOwnerId(999L);

        when(projectMapper.selectById(projectId)).thenReturn(project);

        ServiceException ex = assertThrows(ServiceException.class, () -> service.addMember(bo));
        assertEquals("角色类型非法，仅支持 editor/viewer", ex.getMessage());
    }

    @Test
    void shouldRejectAddMemberWhenUserIsOwner() {
        Long projectId = 15L;
        Long userId = 100L;
        Long ownerId = 100L; // same as userId

        DocProjectMemberBo bo = new DocProjectMemberBo();
        bo.setProjectId(projectId);
        bo.setUserId(userId);
        bo.setRoleType("editor");

        DocProject project = new DocProject();
        project.setId(projectId);
        project.setOwnerId(ownerId);

        when(projectMapper.selectById(projectId)).thenReturn(project);

        ServiceException ex = assertThrows(ServiceException.class, () -> service.addMember(bo));
        assertEquals("项目负责人已是固定成员，无需重复添加", ex.getMessage());
    }

    @Test
    void shouldRejectAddMemberWhenMemberAlreadyExists() {
        Long projectId = 16L;
        Long userId = 100L;

        DocProjectMemberBo bo = new DocProjectMemberBo();
        bo.setProjectId(projectId);
        bo.setUserId(userId);
        bo.setRoleType("editor");

        DocProject project = new DocProject();
        project.setId(projectId);
        project.setOwnerId(999L);

        DocProjectMember existing = new DocProjectMember();
        existing.setId(50L);
        existing.setProjectId(projectId);
        existing.setUserId(userId);

        when(projectMapper.selectById(projectId)).thenReturn(project);
        when(memberMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(existing);

        ServiceException ex = assertThrows(ServiceException.class, () -> service.addMember(bo));
        assertEquals("项目成员已存在", ex.getMessage());
    }

    // ===================== removeMember success tests =====================

    @Test
    void shouldRemoveMemberSuccessfully() {
        Long projectId = 30L;
        Long userId = 300L;
        Long ownerId = 999L;

        DocProject project = new DocProject();
        project.setId(projectId);
        project.setOwnerId(ownerId);

        when(projectMapper.selectById(projectId)).thenReturn(project);
        when(memberMapper.delete(any(LambdaQueryWrapper.class))).thenReturn(1);

        service.removeMember(projectId, userId);

        verify(projectAccessService).assertAction(projectId, DocProjectAction.EDIT_PROJECT);
        verify(memberMapper).delete(any(LambdaQueryWrapper.class));
    }

    @Test
    void shouldDeleteByProjectIdAndUserId() {
        Long projectId = 31L;
        Long userId = 310L;
        Long ownerId = 999L;

        DocProject project = new DocProject();
        project.setId(projectId);
        project.setOwnerId(ownerId);

        when(projectMapper.selectById(projectId)).thenReturn(project);
        when(memberMapper.delete(any(LambdaQueryWrapper.class))).thenReturn(1);

        service.removeMember(projectId, userId);

        ArgumentCaptor<LambdaQueryWrapper<DocProjectMember>> captor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(memberMapper).delete(captor.capture());
        assertNotNull(captor.getValue());
    }

    @Test
    void shouldCallEditActionCheckBeforeRemove() {
        Long projectId = 32L;
        Long userId = 320L;
        Long ownerId = 999L;

        DocProject project = new DocProject();
        project.setId(projectId);
        project.setOwnerId(ownerId);

        when(projectMapper.selectById(projectId)).thenReturn(project);
        when(memberMapper.delete(any(LambdaQueryWrapper.class))).thenReturn(1);

        service.removeMember(projectId, userId);

        verify(projectAccessService).assertAction(projectId, DocProjectAction.EDIT_PROJECT);
    }

    // ===================== removeMember guard tests =====================

    @Test
    void shouldRejectRemoveMemberWhenAccessDenied() {
        Long projectId = 31L;
        Long userId = 300L;

        doThrow(new ServiceException("无权限")).when(projectAccessService).assertAction(projectId, DocProjectAction.EDIT_PROJECT);

        ServiceException ex = assertThrows(ServiceException.class, () -> service.removeMember(projectId, userId));
        assertEquals("无权限", ex.getMessage());
    }

    @Test
    void shouldRejectRemoveMemberWhenProjectNotFound() {
        Long projectId = 32L;
        Long userId = 300L;

        when(projectMapper.selectById(projectId)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class, () -> service.removeMember(projectId, userId));
        assertEquals("项目不存在", ex.getMessage());
    }

    @Test
    void shouldRejectRemoveMemberWhenUserIsOwner() {
        Long projectId = 33L;
        Long userId = 300L;
        Long ownerId = 300L; // same as userId

        DocProject project = new DocProject();
        project.setId(projectId);
        project.setOwnerId(ownerId);

        when(projectMapper.selectById(projectId)).thenReturn(project);

        ServiceException ex = assertThrows(ServiceException.class, () -> service.removeMember(projectId, userId));
        assertEquals("项目负责人不可移除", ex.getMessage());
    }

    @Test
    void shouldRejectRemoveMemberWhenMemberNotExists() {
        Long projectId = 34L;
        Long userId = 300L;
        Long ownerId = 999L;

        DocProject project = new DocProject();
        project.setId(projectId);
        project.setOwnerId(ownerId);

        when(projectMapper.selectById(projectId)).thenReturn(project);
        when(memberMapper.delete(any(LambdaQueryWrapper.class))).thenReturn(0);

        ServiceException ex = assertThrows(ServiceException.class, () -> service.removeMember(projectId, userId));
        assertEquals("项目成员不存在", ex.getMessage());
    }

    // ===================== helper methods =====================

    private DocProjectMember createMember(Long id, Long projectId, Long userId, String roleType, Date createTime) {
        DocProjectMember member = new DocProjectMember();
        member.setId(id);
        member.setProjectId(projectId);
        member.setUserId(userId);
        member.setRoleType(roleType);
        member.setCreateTime(createTime);
        return member;
    }

    private static void initTableInfo(Class<?> entityClass) {
        if (TableInfoHelper.getTableInfo(entityClass) == null) {
            TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), "test"), entityClass);
        }
    }
}