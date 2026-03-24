package org.dromara.docman.controller;

import jakarta.servlet.http.HttpServletResponse;
import org.dromara.common.core.domain.R;
import org.dromara.docman.application.service.DocArchiveApplicationService;
import org.dromara.docman.domain.vo.DocArchivePackageVo;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@Tag("dev")
@Tag("prod")
@Tag("local")
class DocArchiveControllerTest {

    @Mock
    private DocArchiveApplicationService archiveApplicationService;

    @Mock
    private HttpServletResponse response;

    @Test
    void shouldArchiveProjectAndReturnResult() {
        DocArchiveController controller = new DocArchiveController(archiveApplicationService);
        DocArchivePackageVo vo = new DocArchivePackageVo();
        vo.setId(1L);
        vo.setProjectId(100L);
        vo.setArchiveNo("ARCH-20260321-001");
        org.mockito.Mockito.when(archiveApplicationService.archive(100L)).thenReturn(vo);

        R<DocArchivePackageVo> result = controller.archive(100L);

        assertEquals(R.SUCCESS, result.getCode());
        assertEquals(vo, result.getData());
        assertEquals("ARCH-20260321-001", result.getData().getArchiveNo());
        verify(archiveApplicationService).archive(100L);
    }

    @Test
    void shouldGetLatestArchiveForProject() {
        DocArchiveController controller = new DocArchiveController(archiveApplicationService);
        DocArchivePackageVo vo = new DocArchivePackageVo();
        vo.setId(2L);
        vo.setProjectId(200L);
        org.mockito.Mockito.when(archiveApplicationService.getLatest(200L)).thenReturn(vo);

        R<DocArchivePackageVo> result = controller.getArchive(200L);

        assertEquals(R.SUCCESS, result.getCode());
        assertEquals(vo, result.getData());
        verify(archiveApplicationService).getLatest(200L);
    }

    @Test
    void shouldReturnNullWhenNoArchiveExists() {
        DocArchiveController controller = new DocArchiveController(archiveApplicationService);
        org.mockito.Mockito.when(archiveApplicationService.getLatest(999L)).thenReturn(null);

        R<DocArchivePackageVo> result = controller.getArchive(999L);

        assertEquals(R.SUCCESS, result.getCode());
        assertNull(result.getData());
        verify(archiveApplicationService).getLatest(999L);
    }

    @Test
    void shouldGetArchiveHistoryForProject() {
        DocArchiveController controller = new DocArchiveController(archiveApplicationService);
        DocArchivePackageVo vo1 = new DocArchivePackageVo();
        vo1.setId(1L);
        vo1.setArchiveVersion(1L);
        DocArchivePackageVo vo2 = new DocArchivePackageVo();
        vo2.setId(2L);
        vo2.setArchiveVersion(2L);
        List<DocArchivePackageVo> history = List.of(vo1, vo2);
        org.mockito.Mockito.when(archiveApplicationService.listHistory(300L)).thenReturn(history);

        R<List<DocArchivePackageVo>> result = controller.getArchiveHistory(300L);

        assertEquals(R.SUCCESS, result.getCode());
        assertEquals(2, result.getData().size());
        assertEquals(1L, result.getData().get(0).getArchiveVersion());
        verify(archiveApplicationService).listHistory(300L);
    }

    @Test
    void shouldDownloadArchive() {
        DocArchiveController controller = new DocArchiveController(archiveApplicationService);

        controller.download(1L, response);

        verify(archiveApplicationService).downloadArchive(1L, response);
    }
}