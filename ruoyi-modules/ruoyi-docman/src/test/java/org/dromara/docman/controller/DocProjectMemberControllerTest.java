package org.dromara.docman.controller;

import org.dromara.common.core.domain.R;
import org.dromara.docman.application.service.DocProjectMemberApplicationService;
import org.dromara.docman.application.service.DocProjectMemberQueryApplicationService;
import org.dromara.docman.domain.bo.DocProjectMemberBo;
import org.dromara.docman.domain.vo.DocProjectMemberVo;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("dev")
@Tag("prod")
@Tag("local")
class DocProjectMemberControllerTest {

    @Mock
    private DocProjectMemberApplicationService projectMemberApplicationService;

    @Mock
    private DocProjectMemberQueryApplicationService projectMemberQueryApplicationService;

    @Test
    void shouldDelegateListQuery() {
        DocProjectMemberController controller = new DocProjectMemberController(projectMemberApplicationService, projectMemberQueryApplicationService);
        Long projectId = 1L;
        DocProjectMemberVo member = new DocProjectMemberVo();
        member.setProjectId(projectId);
        member.setUserId(100L);
        List<DocProjectMemberVo> expected = List.of(member);
        when(projectMemberQueryApplicationService.list(projectId)).thenReturn(expected);

        R<List<DocProjectMemberVo>> result = controller.list(projectId);

        assertEquals(R.SUCCESS, result.getCode());
        assertEquals(expected, result.getData());
        verify(projectMemberQueryApplicationService).list(projectId);
    }

    @Test
    void shouldAddMemberAndReturnSuccess() {
        DocProjectMemberController controller = new DocProjectMemberController(projectMemberApplicationService, projectMemberQueryApplicationService);
        Long projectId = 1L;
        DocProjectMemberBo bo = new DocProjectMemberBo();
        bo.setUserId(100L);
        bo.setRoleType("editor");

        R<Void> result = controller.add(projectId, bo);

        assertEquals(R.SUCCESS, result.getCode());
        verify(projectMemberApplicationService).add(projectId, bo);
    }

    @Test
    void shouldRemoveMemberAndReturnSuccess() {
        DocProjectMemberController controller = new DocProjectMemberController(projectMemberApplicationService, projectMemberQueryApplicationService);
        Long projectId = 1L;
        Long userId = 100L;

        R<Void> result = controller.remove(projectId, userId);

        assertEquals(R.SUCCESS, result.getCode());
        verify(projectMemberApplicationService).remove(projectId, userId);
    }
}