package org.dromara.docman.application.service;

import jakarta.servlet.http.HttpServletResponse;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.docman.application.assembler.DocArchiveAssembler;
import org.dromara.docman.domain.entity.DocArchivePackage;
import org.dromara.docman.domain.vo.DocArchivePackageVo;
import org.dromara.docman.service.IDocArchiveService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletResponse;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("dev")
@Tag("prod")
@Tag("local")
class DocArchiveApplicationServiceTest {

    @Mock
    private IDocArchiveService archiveService;

    @Mock
    private DocArchiveAssembler archiveAssembler;

    @InjectMocks
    private DocArchiveApplicationService applicationService;

    @TempDir
    Path tempDir;

    @Test
    void shouldDelegateToArchiveService_whenArchive() {
        Long projectId = 1L;
        DocArchivePackage archivePackage = new DocArchivePackage();
        archivePackage.setId(100L);
        DocArchivePackageVo expectedVo = new DocArchivePackageVo();
        expectedVo.setId(100L);

        when(archiveService.archiveProject(projectId)).thenReturn(archivePackage);
        when(archiveAssembler.toVo(archivePackage)).thenReturn(expectedVo);

        DocArchivePackageVo result = applicationService.archive(projectId);

        assertNotNull(result);
        assertEquals(100L, result.getId());
        verify(archiveService).archiveProject(projectId);
        verify(archiveAssembler).toVo(archivePackage);
    }

    @Test
    void shouldDelegateToArchiveService_whenGetLatest() {
        Long projectId = 1L;
        DocArchivePackage archivePackage = new DocArchivePackage();
        archivePackage.setId(100L);
        DocArchivePackageVo expectedVo = new DocArchivePackageVo();
        expectedVo.setId(100L);

        when(archiveService.getByProjectId(projectId)).thenReturn(archivePackage);
        when(archiveAssembler.toVo(archivePackage)).thenReturn(expectedVo);

        DocArchivePackageVo result = applicationService.getLatest(projectId);

        assertNotNull(result);
        assertEquals(100L, result.getId());
        verify(archiveService).getByProjectId(projectId);
        verify(archiveAssembler).toVo(archivePackage);
    }

    @Test
    void shouldDelegateToArchiveService_whenListHistory() {
        Long projectId = 1L;
        DocArchivePackage archivePackage = new DocArchivePackage();
        archivePackage.setId(100L);
        List<DocArchivePackage> packages = Collections.singletonList(archivePackage);
        DocArchivePackageVo expectedVo = new DocArchivePackageVo();
        expectedVo.setId(100L);
        List<DocArchivePackageVo> expectedVoList = Collections.singletonList(expectedVo);

        when(archiveService.listByProjectId(projectId)).thenReturn(packages);
        when(archiveAssembler.toVoList(packages)).thenReturn(expectedVoList);

        List<DocArchivePackageVo> result = applicationService.listHistory(projectId);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(100L, result.get(0).getId());
        verify(archiveService).listByProjectId(projectId);
        verify(archiveAssembler).toVoList(packages);
    }

    @Test
    void shouldThrowException_whenDownloadArchive_andArchiveNotFound() {
        when(archiveService.getById(anyLong())).thenReturn(null);

        MockHttpServletResponse response = new MockHttpServletResponse();

        ServiceException ex = assertThrows(ServiceException.class,
            () -> applicationService.downloadArchive(1L, response));

        assertEquals("归档包不存在", ex.getMessage());
    }

    @Test
    void shouldThrowException_whenDownloadArchive_andArchiveNotCompleted() {
        DocArchivePackage archivePackage = new DocArchivePackage();
        archivePackage.setId(1L);
        archivePackage.setStatus("generating");

        when(archiveService.getById(1L)).thenReturn(archivePackage);

        MockHttpServletResponse response = new MockHttpServletResponse();

        ServiceException ex = assertThrows(ServiceException.class,
            () -> applicationService.downloadArchive(1L, response));

        assertEquals("归档未完成，无法下载", ex.getMessage());
    }

    @Test
    void shouldThrowException_whenDownloadArchive_andArchivePathIsBlank() {
        DocArchivePackage archivePackage = new DocArchivePackage();
        archivePackage.setId(1L);
        archivePackage.setStatus("completed");
        archivePackage.setNasArchivePath("");

        when(archiveService.getById(1L)).thenReturn(archivePackage);

        MockHttpServletResponse response = new MockHttpServletResponse();

        ServiceException ex = assertThrows(ServiceException.class,
            () -> applicationService.downloadArchive(1L, response));

        assertEquals("归档文件路径为空", ex.getMessage());
    }

    @Test
    void shouldThrowException_whenDownloadArchive_andFileNotExists() {
        DocArchivePackage archivePackage = new DocArchivePackage();
        archivePackage.setId(1L);
        archivePackage.setStatus("completed");
        archivePackage.setNasArchivePath("/non/existent/path.zip");

        when(archiveService.getById(1L)).thenReturn(archivePackage);

        MockHttpServletResponse response = new MockHttpServletResponse();

        ServiceException ex = assertThrows(ServiceException.class,
            () -> applicationService.downloadArchive(1L, response));

        assertEquals("归档文件不存在: /non/existent/path.zip", ex.getMessage());
    }

    @Test
    void shouldThrowException_whenDownloadArchive_andPathIsDirectory() throws Exception {
        Path dirPath = tempDir.resolve("archive-dir");
        Files.createDirectories(dirPath);

        DocArchivePackage archivePackage = new DocArchivePackage();
        archivePackage.setId(1L);
        archivePackage.setStatus("completed");
        archivePackage.setNasArchivePath(dirPath.toString());

        when(archiveService.getById(1L)).thenReturn(archivePackage);

        MockHttpServletResponse response = new MockHttpServletResponse();

        ServiceException ex = assertThrows(ServiceException.class,
            () -> applicationService.downloadArchive(1L, response));

        assertEquals("归档文件不存在: " + dirPath.toString(), ex.getMessage());
    }

    @Test
    void shouldDownloadSuccessfully_whenArchiveValid() throws Exception {
        Path archiveFile = tempDir.resolve("test-archive.zip");
        byte[] content = "test-zip-content".getBytes(StandardCharsets.UTF_8);
        Files.write(archiveFile, content);

        DocArchivePackage archivePackage = new DocArchivePackage();
        archivePackage.setId(1L);
        archivePackage.setStatus("completed");
        archivePackage.setNasArchivePath(archiveFile.toString());
        archivePackage.setArchiveNo("ARCH-2024-001");

        when(archiveService.getById(1L)).thenReturn(archivePackage);

        MockHttpServletResponse response = new MockHttpServletResponse();

        applicationService.downloadArchive(1L, response);

        assertEquals("application/zip", response.getContentType());
        assertEquals(content.length, response.getContentLength());
        assertTrue(response.getHeader("Content-Disposition").contains("ARCH-2024-001.zip"));
    }

    @Test
    void shouldUseIdAsFileName_whenArchiveNoIsBlank() throws Exception {
        Path archiveFile = tempDir.resolve("archive-no-number.zip");
        byte[] content = "content".getBytes(StandardCharsets.UTF_8);
        Files.write(archiveFile, content);

        DocArchivePackage archivePackage = new DocArchivePackage();
        archivePackage.setId(999L);
        archivePackage.setStatus("completed");
        archivePackage.setNasArchivePath(archiveFile.toString());
        archivePackage.setArchiveNo(null);

        when(archiveService.getById(999L)).thenReturn(archivePackage);

        MockHttpServletResponse response = new MockHttpServletResponse();

        applicationService.downloadArchive(999L, response);

        assertTrue(response.getHeader("Content-Disposition").contains("archive-999.zip"));
    }

    @Test
    void shouldNotAppendZipExtension_whenArchiveNoAlreadyEndsWithZip() throws Exception {
        Path archiveFile = tempDir.resolve("archive-with-zip.zip");
        byte[] content = "content".getBytes(StandardCharsets.UTF_8);
        Files.write(archiveFile, content);

        DocArchivePackage archivePackage = new DocArchivePackage();
        archivePackage.setId(1L);
        archivePackage.setStatus("completed");
        archivePackage.setNasArchivePath(archiveFile.toString());
        archivePackage.setArchiveNo("ARCH-2024-001.zip");

        when(archiveService.getById(1L)).thenReturn(archivePackage);

        MockHttpServletResponse response = new MockHttpServletResponse();

        applicationService.downloadArchive(1L, response);

        assertTrue(response.getHeader("Content-Disposition").contains("ARCH-2024-001.zip"));
    }
}