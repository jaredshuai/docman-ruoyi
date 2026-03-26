package org.dromara.docman.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.redis.utils.RedisUtils;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.docman.domain.entity.DocProject;
import org.dromara.docman.domain.entity.DocProjectMember;
import org.dromara.docman.domain.enums.DocProjectAction;
import org.dromara.docman.domain.enums.DocProjectRole;
import org.dromara.docman.domain.service.DocProjectPermissionPolicy;
import org.dromara.docman.mapper.DocProjectMapper;
import org.dromara.docman.mapper.DocProjectMemberMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("dev")
@Tag("prod")
@Tag("local")
class DocProjectAccessServiceImplTest {

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
    private DocProjectPermissionPolicy permissionPolicy;

    @InjectMocks
    private DocProjectAccessServiceImpl service;

    @Test
    void shouldReturnAllProjectIdsForSuperAdmin() {
        try (MockedStatic<LoginHelper> loginHelper = mockStatic(LoginHelper.class)) {
            loginHelper.when(() -> LoginHelper.isSuperAdmin(88L)).thenReturn(true);
            when(projectMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(project(1L), project(2L)));

            List<Long> result = service.listAccessibleProjectIds(88L);

            assertEquals(List.of(1L, 2L), result);
            verify(memberMapper, never()).selectList(any(LambdaQueryWrapper.class));
        }
    }

    @Test
    void shouldReturnMemberProjectIdsForRegularUser() {
        try (MockedStatic<LoginHelper> loginHelper = mockStatic(LoginHelper.class)) {
            loginHelper.when(() -> LoginHelper.isSuperAdmin(66L)).thenReturn(false);
            when(memberMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(member(9L, DocProjectRole.EDITOR)));

            List<Long> result = service.listAccessibleProjectIds(66L);

            assertEquals(List.of(9L), result);
            verify(projectMapper, never()).selectList(any(LambdaQueryWrapper.class));
        }
    }

    @Test
    void shouldReturnOwnerRoleForCurrentSuperAdmin() {
        try (MockedStatic<LoginHelper> loginHelper = mockStatic(LoginHelper.class)) {
            loginHelper.when(LoginHelper::isSuperAdmin).thenReturn(true);

            DocProjectRole role = service.getCurrentRole(5L);

            assertEquals(DocProjectRole.OWNER, role);
            verify(memberMapper, never()).selectOne(any(LambdaQueryWrapper.class));
        }
    }

    @Test
    void shouldRejectCurrentRoleWhenUserIsNotMember() {
        try (MockedStatic<LoginHelper> loginHelper = mockStatic(LoginHelper.class)) {
            loginHelper.when(LoginHelper::isSuperAdmin).thenReturn(false);
            loginHelper.when(LoginHelper::getUserId).thenReturn(12L);
            when(memberMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

            ServiceException ex = assertThrows(ServiceException.class, () -> service.getCurrentRole(7L));

            assertEquals("你无权访问该项目", ex.getMessage());
        }
    }

    @Test
    void shouldRejectActionWhenPermissionPolicyDeniesRole() {
        try (MockedStatic<LoginHelper> loginHelper = mockStatic(LoginHelper.class)) {
            loginHelper.when(LoginHelper::isSuperAdmin).thenReturn(false);
            loginHelper.when(LoginHelper::getUserId).thenReturn(18L);
            when(memberMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(member(30L, DocProjectRole.VIEWER));
            when(permissionPolicy.can(DocProjectRole.VIEWER, DocProjectAction.EDIT_PROJECT)).thenReturn(false);

            ServiceException ex = assertThrows(ServiceException.class,
                () -> service.assertAction(30L, DocProjectAction.EDIT_PROJECT));

            assertEquals("当前角色[项目只读]无权限执行操作: edit:project", ex.getMessage());
        }
    }

    @Test
    void shouldAllowActionWhenPermissionPolicyGrantsRole() {
        try (MockedStatic<LoginHelper> loginHelper = mockStatic(LoginHelper.class)) {
            loginHelper.when(LoginHelper::isSuperAdmin).thenReturn(false);
            loginHelper.when(LoginHelper::getUserId).thenReturn(21L);
            when(memberMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(member(40L, DocProjectRole.EDITOR));
            when(permissionPolicy.can(DocProjectRole.EDITOR, DocProjectAction.VIEW_DOCUMENT)).thenReturn(true);

            service.assertAction(40L, DocProjectAction.VIEW_DOCUMENT);

            verify(permissionPolicy).can(DocProjectRole.EDITOR, DocProjectAction.VIEW_DOCUMENT);
        }
    }

    private DocProject project(Long id) {
        DocProject project = new DocProject();
        project.setId(id);
        return project;
    }

    private DocProjectMember member(Long projectId, DocProjectRole role) {
        DocProjectMember member = new DocProjectMember();
        member.setProjectId(projectId);
        member.setRoleType(role.getCode());
        return member;
    }

    private static void initTableInfo(Class<?> entityClass) {
        if (TableInfoHelper.getTableInfo(entityClass) == null) {
            TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), "test"), entityClass);
        }
    }

    // ========== evictAccessibleProjectsCache tests ==========

    @Nested
    @DisplayName("evictAccessibleProjectsCache() 缓存失效测试")
    class EvictAccessibleProjectsCache {

        @Test
        @DisplayName("正常删除用户可访问项目缓存")
        void shouldEvictCacheForUserIds() {
            try (MockedStatic<RedisUtils> redisMock = mockStatic(RedisUtils.class)) {
                service.evictAccessibleProjectsCache(List.of(1L, 2L, 3L));

                redisMock.verify(() -> RedisUtils.deleteObject(anyList()));
            }
        }

        @Test
        @DisplayName("空用户列表时不执行删除")
        void shouldNotEvictWhenUserIdsEmpty() {
            try (MockedStatic<RedisUtils> redisMock = mockStatic(RedisUtils.class)) {
                service.evictAccessibleProjectsCache(List.of());

                redisMock.verifyNoInteractions();
            }
        }

        @Test
        @DisplayName("null 用户列表时不执行删除")
        void shouldNotEvictWhenUserIdsNull() {
            try (MockedStatic<RedisUtils> redisMock = mockStatic(RedisUtils.class)) {
                service.evictAccessibleProjectsCache(null);

                redisMock.verifyNoInteractions();
            }
        }
    }

    // ========== evictProjectRoleCache tests ==========

    @Nested
    @DisplayName("evictProjectRoleCache() 缓存失效测试")
    class EvictProjectRoleCache {

        @Test
        @DisplayName("正常删除用户项目角色缓存")
        void shouldEvictCacheForProjectAndUserIds() {
            try (MockedStatic<RedisUtils> redisMock = mockStatic(RedisUtils.class)) {
                service.evictProjectRoleCache(100L, List.of(1L, 2L));

                redisMock.verify(() -> RedisUtils.deleteObject(anyList()));
            }
        }

        @Test
        @DisplayName("空用户列表时不执行删除")
        void shouldNotEvictWhenUserIdsEmpty() {
            try (MockedStatic<RedisUtils> redisMock = mockStatic(RedisUtils.class)) {
                service.evictProjectRoleCache(100L, List.of());

                redisMock.verifyNoInteractions();
            }
        }

        @Test
        @DisplayName("null 项目ID时不执行删除")
        void shouldNotEvictWhenProjectIdNull() {
            try (MockedStatic<RedisUtils> redisMock = mockStatic(RedisUtils.class)) {
                service.evictProjectRoleCache(null, List.of(1L));

                redisMock.verifyNoInteractions();
            }
        }
    }
}
