package org.dromara.docman.application.service;

import org.dromara.docman.domain.bo.DocProjectMemberBo;
import org.dromara.docman.service.IDocProjectMemberService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@Tag("dev")
@Tag("prod")
@Tag("local")
class DocProjectMemberApplicationServiceTest {

    @Mock
    private IDocProjectMemberService projectMemberService;

    @InjectMocks
    private DocProjectMemberApplicationService applicationService;

    @Test
    void shouldBackfillProjectIdWhenAddingMember() {
        Long projectId = 100L;
        DocProjectMemberBo bo = new DocProjectMemberBo();
        bo.setUserId(1L);
        bo.setRoleType("admin");

        applicationService.add(projectId, bo);

        assertEquals(projectId, bo.getProjectId());
        verify(projectMemberService).addMember(bo);
    }

    @Test
    void shouldDelegateToRemoveMember() {
        Long projectId = 200L;
        Long userId = 2L;

        applicationService.remove(projectId, userId);

        verify(projectMemberService).removeMember(projectId, userId);
    }
}