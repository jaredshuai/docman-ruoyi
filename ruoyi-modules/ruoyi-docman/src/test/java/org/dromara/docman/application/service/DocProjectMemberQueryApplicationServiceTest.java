package org.dromara.docman.application.service;

import org.dromara.docman.application.assembler.DocProjectMemberAssembler;
import org.dromara.docman.domain.entity.DocProjectMember;
import org.dromara.docman.domain.vo.DocProjectMemberVo;
import org.dromara.docman.service.IDocProjectMemberService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("dev")
@Tag("prod")
@Tag("local")
class DocProjectMemberQueryApplicationServiceTest {

    @Mock
    private IDocProjectMemberService projectMemberService;

    @Mock
    private DocProjectMemberAssembler projectMemberAssembler;

    @InjectMocks
    private DocProjectMemberQueryApplicationService queryApplicationService;

    @Test
    void shouldDelegateListByProjectIdWhenListing() {
        Long projectId = 100L;
        DocProjectMember member = new DocProjectMember();
        member.setId(1L);
        member.setProjectId(projectId);
        member.setUserId(10L);
        member.setRoleType("editor");
        List<DocProjectMember> members = List.of(member);

        when(projectMemberService.listByProjectId(projectId)).thenReturn(members);

        queryApplicationService.list(projectId);

        verify(projectMemberService).listByProjectId(projectId);
    }

    @Test
    void shouldMapEntitiesToVoListThroughAssembler() {
        Long projectId = 200L;
        DocProjectMember member1 = new DocProjectMember();
        member1.setId(1L);
        member1.setProjectId(projectId);
        member1.setUserId(10L);
        member1.setRoleType("owner");

        DocProjectMember member2 = new DocProjectMember();
        member2.setId(2L);
        member2.setProjectId(projectId);
        member2.setUserId(20L);
        member2.setRoleType("viewer");

        List<DocProjectMember> members = List.of(member1, member2);

        DocProjectMemberVo vo1 = new DocProjectMemberVo();
        vo1.setId(1L);
        vo1.setProjectId(projectId);
        vo1.setUserId(10L);
        vo1.setRoleType("owner");

        DocProjectMemberVo vo2 = new DocProjectMemberVo();
        vo2.setId(2L);
        vo2.setProjectId(projectId);
        vo2.setUserId(20L);
        vo2.setRoleType("viewer");

        List<DocProjectMemberVo> expectedVos = List.of(vo1, vo2);

        when(projectMemberService.listByProjectId(projectId)).thenReturn(members);
        when(projectMemberAssembler.toVoList(members)).thenReturn(expectedVos);

        List<DocProjectMemberVo> result = queryApplicationService.list(projectId);

        verify(projectMemberAssembler).toVoList(members);
        assertSame(expectedVos, result);
        assertEquals(2, result.size());
    }

    @Test
    void shouldReturnEmptyListWhenNoMembersFound() {
        Long projectId = 300L;
        List<DocProjectMember> emptyMembers = Collections.emptyList();
        List<DocProjectMemberVo> emptyVos = Collections.emptyList();

        when(projectMemberService.listByProjectId(projectId)).thenReturn(emptyMembers);
        when(projectMemberAssembler.toVoList(emptyMembers)).thenReturn(emptyVos);

        List<DocProjectMemberVo> result = queryApplicationService.list(projectId);

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(projectMemberService).listByProjectId(projectId);
        verify(projectMemberAssembler).toVoList(emptyMembers);
    }
}