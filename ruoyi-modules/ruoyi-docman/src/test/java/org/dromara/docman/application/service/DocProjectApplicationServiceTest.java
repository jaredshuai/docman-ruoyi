package org.dromara.docman.application.service;

import org.dromara.docman.domain.bo.DocProjectBo;
import org.dromara.docman.service.IDocProjectService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
@Tag("dev")
@Tag("prod")
@Tag("local")
class DocProjectApplicationServiceTest {

    @Mock
    private IDocProjectService projectService;

    @InjectMocks
    private DocProjectApplicationService applicationService;

    @Test
    void create_shouldDelegateToProjectService() {
        // Arrange
        DocProjectBo bo = new DocProjectBo();
        bo.setName("Test Project");
        bo.setCustomerType("enterprise");
        bo.setBusinessType("software");
        bo.setDocumentCategory("technical");
        bo.setOwnerId(1L);

        Long expectedId = 100L;
        when(projectService.insertProject(bo)).thenReturn(expectedId);

        // Act
        Long result = applicationService.create(bo);

        // Assert
        assertEquals(expectedId, result);
        verify(projectService).insertProject(bo);
    }

    @Test
    void update_shouldDelegateToProjectService() {
        // Arrange
        DocProjectBo bo = new DocProjectBo();
        bo.setId(1L);
        bo.setName("Updated Project");
        bo.setCustomerType("enterprise");
        bo.setBusinessType("software");
        bo.setDocumentCategory("technical");
        bo.setOwnerId(1L);

        // Act
        applicationService.update(bo);

        // Assert
        verify(projectService).updateProject(bo);
    }

    @Test
    void delete_shouldDelegateToProjectService() {
        // Arrange
        List<Long> ids = Arrays.asList(1L, 2L, 3L);

        // Act
        applicationService.delete(ids);

        // Assert
        verify(projectService).deleteByIds(ids);
    }
}