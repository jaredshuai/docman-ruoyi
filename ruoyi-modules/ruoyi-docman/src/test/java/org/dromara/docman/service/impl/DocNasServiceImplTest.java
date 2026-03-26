package org.dromara.docman.service.impl;

import org.dromara.common.oss.core.OssClient;
import org.dromara.common.oss.entity.UploadResult;
import org.dromara.common.oss.factory.OssFactory;
import org.dromara.system.domain.SysOss;
import org.dromara.system.mapper.SysOssMapper;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.MockedStatic;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("dev")
@Tag("prod")
@Tag("local")
class DocNasServiceImplTest {

    @Mock
    private SysOssMapper sysOssMapper;

    @InjectMocks
    private DocNasServiceImpl service;

    // ==================== buildProjectBasePath tests ====================

    @Test
    void buildProjectBasePath_shouldMapTelecomToChineseLabel() {
        int year = LocalDate.now().getYear();

        String result = service.buildProjectBasePath("telecom", "TestProject");

        assertEquals(String.format("/项目文档/%d/电信/TestProject", year), result);
    }

    @Test
    void buildProjectBasePath_shouldMapSocialToChineseLabel() {
        int year = LocalDate.now().getYear();

        String result = service.buildProjectBasePath("social", "AnotherProject");

        assertEquals(String.format("/项目文档/%d/社会客户/AnotherProject", year), result);
    }

    @Test
    void buildProjectBasePath_shouldPassthroughUnknownCustomerType() {
        int year = LocalDate.now().getYear();

        String result = service.buildProjectBasePath("unknown", "ProjectX");

        assertEquals(String.format("/项目文档/%d/unknown/ProjectX", year), result);
    }

    // ==================== createProjectDirectory tests ====================

    @Test
    void createProjectDirectory_shouldNormalizeLeadingSlashAndUploadKeepFile() {
        OssClient mockClient = mock(OssClient.class);
        try (MockedStatic<OssFactory> ossFactoryMock = mockStatic(OssFactory.class)) {
            ossFactoryMock.when(OssFactory::instance).thenReturn(mockClient);

            boolean result = service.createProjectDirectory("/path/to/project");

            assertTrue(result);
            ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
            verify(mockClient).upload(any(ByteArrayInputStream.class), keyCaptor.capture(), anyLong(), anyString());
            assertEquals("path/to/project/.keep", keyCaptor.getValue());
        }
    }

    @Test
    void createProjectDirectory_shouldHandlePathWithoutLeadingSlash() {
        OssClient mockClient = mock(OssClient.class);
        try (MockedStatic<OssFactory> ossFactoryMock = mockStatic(OssFactory.class)) {
            ossFactoryMock.when(OssFactory::instance).thenReturn(mockClient);

            boolean result = service.createProjectDirectory("path/to/project");

            assertTrue(result);
            ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
            verify(mockClient).upload(any(ByteArrayInputStream.class), keyCaptor.capture(), anyLong(), anyString());
            assertEquals("path/to/project/.keep", keyCaptor.getValue());
        }
    }

    @Test
    void createProjectDirectory_shouldReturnFalseWhenOssFactoryThrowsException() {
        try (MockedStatic<OssFactory> ossFactoryMock = mockStatic(OssFactory.class)) {
            ossFactoryMock.when(OssFactory::instance).thenThrow(new RuntimeException("OSS not configured"));

            boolean result = service.createProjectDirectory("/some/path");

            assertFalse(result);
        }
    }

    @Test
    void createProjectDirectory_shouldReturnFalseWhenUploadThrowsException() {
        OssClient mockClient = mock(OssClient.class);
        try (MockedStatic<OssFactory> ossFactoryMock = mockStatic(OssFactory.class)) {
            ossFactoryMock.when(OssFactory::instance).thenReturn(mockClient);
            when(mockClient.upload(any(ByteArrayInputStream.class), anyString(), anyLong(), anyString()))
                .thenThrow(new RuntimeException("Upload failed"));

            boolean result = service.createProjectDirectory("/some/path");

            assertFalse(result);
        }
    }

    // ==================== createNodeDirectory tests ====================

    @Test
    void createNodeDirectory_shouldBuildCombinedPathAndDelegate() {
        OssClient mockClient = mock(OssClient.class);
        try (MockedStatic<OssFactory> ossFactoryMock = mockStatic(OssFactory.class)) {
            ossFactoryMock.when(OssFactory::instance).thenReturn(mockClient);

            boolean result = service.createNodeDirectory("/project/base", "subfolder");

            assertTrue(result);
            ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
            verify(mockClient).upload(any(ByteArrayInputStream.class), keyCaptor.capture(), anyLong(), anyString());
            assertEquals("project/base/subfolder/.keep", keyCaptor.getValue());
        }
    }

    // ==================== uploadFile tests ====================

    @Test
    void uploadFile_shouldNormalizeKeyAndReturnOssId() {
        OssClient mockClient = mock(OssClient.class);
        UploadResult uploadResult = UploadResult.builder()
            .url("http://oss.example.com/bucket/uploads/file.pdf")
            .filename("uploads/file.pdf")
            .eTag("abc123")
            .build();

        try (MockedStatic<OssFactory> ossFactoryMock = mockStatic(OssFactory.class)) {
            ossFactoryMock.when(OssFactory::instance).thenReturn(mockClient);
            when(mockClient.upload(any(ByteArrayInputStream.class), anyString(), anyLong(), anyString()))
                .thenReturn(uploadResult);
            when(mockClient.getConfigKey()).thenReturn("minio");
            // 模拟 insert 设置 ossId
            doAnswer(invocation -> {
                SysOss oss = invocation.getArgument(0);
                oss.setOssId(12345L);
                return 1;
            }).when(sysOssMapper).insert(any(SysOss.class));

            byte[] fileBytes = new byte[]{1, 2, 3, 4, 5};
            Long result = service.uploadFile("/uploads/file.pdf", fileBytes, "file.pdf");

            assertNotNull(result);
            assertEquals(12345L, result);

            ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
            verify(mockClient).upload(any(ByteArrayInputStream.class), keyCaptor.capture(), anyLong(), anyString());
            assertEquals("uploads/file.pdf", keyCaptor.getValue());

            // 验证 SysOss 记录被正确创建
            ArgumentCaptor<SysOss> ossCaptor = ArgumentCaptor.forClass(SysOss.class);
            verify(sysOssMapper).insert(ossCaptor.capture());
            SysOss savedOss = ossCaptor.getValue();
            assertEquals("file.pdf", savedOss.getOriginalName());
            assertEquals(".pdf", savedOss.getFileSuffix());
            assertEquals("minio", savedOss.getService());
        }
    }

    @Test
    void uploadFile_shouldHandlePathWithoutLeadingSlash() {
        OssClient mockClient = mock(OssClient.class);
        UploadResult uploadResult = UploadResult.builder()
            .url("http://oss.example.com/bucket/uploads/file.doc")
            .filename("uploads/file.doc")
            .eTag("def456")
            .build();

        try (MockedStatic<OssFactory> ossFactoryMock = mockStatic(OssFactory.class)) {
            ossFactoryMock.when(OssFactory::instance).thenReturn(mockClient);
            when(mockClient.upload(any(ByteArrayInputStream.class), anyString(), anyLong(), anyString()))
                .thenReturn(uploadResult);
            when(mockClient.getConfigKey()).thenReturn("minio");
            doAnswer(invocation -> {
                SysOss oss = invocation.getArgument(0);
                oss.setOssId(67890L);
                return 1;
            }).when(sysOssMapper).insert(any(SysOss.class));

            byte[] fileBytes = new byte[]{1, 2, 3};
            Long result = service.uploadFile("uploads/file.doc", fileBytes, "file.doc");

            assertNotNull(result);
            assertEquals(67890L, result);
        }
    }

    @Test
    void uploadFile_shouldReturnNullOnUploadException() {
        OssClient mockClient = mock(OssClient.class);
        try (MockedStatic<OssFactory> ossFactoryMock = mockStatic(OssFactory.class)) {
            ossFactoryMock.when(OssFactory::instance).thenReturn(mockClient);
            when(mockClient.upload(any(ByteArrayInputStream.class), anyString(), anyLong(), anyString()))
                .thenThrow(new RuntimeException("Network error"));

            byte[] fileBytes = new byte[]{1, 2, 3};
            Long result = service.uploadFile("/uploads/file.pdf", fileBytes, "file.pdf");

            assertNull(result);
        }
    }

    @Test
    void uploadFile_shouldReturnNullWhenOssFactoryThrowsException() {
        try (MockedStatic<OssFactory> ossFactoryMock = mockStatic(OssFactory.class)) {
            ossFactoryMock.when(OssFactory::instance).thenThrow(new RuntimeException("OSS not configured"));

            byte[] fileBytes = new byte[]{1, 2, 3};
            Long result = service.uploadFile("/uploads/file.pdf", fileBytes, "file.pdf");

            assertNull(result);
        }
    }
}