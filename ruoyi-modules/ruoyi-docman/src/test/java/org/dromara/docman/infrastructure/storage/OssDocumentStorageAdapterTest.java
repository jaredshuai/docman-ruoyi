package org.dromara.docman.infrastructure.storage;

import org.dromara.common.oss.core.OssClient;
import org.dromara.common.oss.entity.UploadResult;
import org.dromara.common.oss.factory.OssFactory;
import org.dromara.system.domain.SysOss;
import org.dromara.system.mapper.SysOssMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

@Tag("dev")
@Tag("prod")
@Tag("local")
class OssDocumentStorageAdapterTest {

    @TempDir
    Path tempDir;

    @AfterEach
    void tearDown() {
        System.clearProperty("docman.storage.localOnly");
        System.clearProperty("docman.storage.preferLocal");
        System.clearProperty("docman.upload.localRoot");
    }

    @Test
    void shouldCreateDirectoryLocallyWhenLocalOnlyIsEnabled() {
        System.setProperty("docman.storage.localOnly", "true");
        System.setProperty("docman.upload.localRoot", tempDir.toString());

        OssDocumentStorageAdapter adapter = new OssDocumentStorageAdapter(mock(SysOssMapper.class));

        boolean result = adapter.ensureDirectory("/项目文档/2026/电信/demo-project");

        assertTrue(result);
        assertTrue(Files.isDirectory(tempDir.resolve("项目文档").resolve("2026").resolve("电信").resolve("demo-project")));
    }

    @Test
    void shouldStoreAndLoadLocallyWhenPreferLocalIsEnabled() {
        System.setProperty("docman.storage.preferLocal", "true");
        System.setProperty("docman.upload.localRoot", tempDir.toString());

        OssDocumentStorageAdapter adapter = new OssDocumentStorageAdapter(mock(SysOssMapper.class));
        byte[] expected = "local-doc-content".getBytes(StandardCharsets.UTF_8);

        var stored = adapter.store("/demo/test.txt", expected, "test.txt", "text/plain");
        byte[] actual = adapter.load("/demo/test.txt");

        assertEquals("/demo/test.txt", stored.path());
        assertEquals("test.txt", stored.fileName());
        assertNull(stored.storageRecordId());
        assertArrayEquals(expected, actual);
    }

    // ==================== OSS Mode Tests ====================

    @Nested
    @DisplayName("OSS 模式测试")
    class OssModeTests {

        @Test
        @DisplayName("store() OSS上传成功返回ossId")
        void shouldReturnOssIdWhenOssUploadSuccess() {
            System.setProperty("docman.upload.localRoot", tempDir.toString());

            SysOssMapper mockMapper = mock(SysOssMapper.class);
            OssClient mockClient = mock(OssClient.class);
            UploadResult uploadResult = UploadResult.builder()
                .url("http://oss.example.com/bucket/demo/test.txt")
                .filename("demo/test.txt")
                .eTag("abc123")
                .build();

            doAnswer(invocation -> {
                SysOss oss = invocation.getArgument(0);
                oss.setOssId(12345L);
                return 1;
            }).when(mockMapper).insert(any(SysOss.class));

            try (MockedStatic<OssFactory> ossFactoryMock = mockStatic(OssFactory.class)) {
                ossFactoryMock.when(OssFactory::instance).thenReturn(mockClient);
                when(mockClient.upload(any(), anyString(), anyLong(), anyString())).thenReturn(uploadResult);
                when(mockClient.getConfigKey()).thenReturn("minio");

                OssDocumentStorageAdapter adapter = new OssDocumentStorageAdapter(mockMapper);
                byte[] content = "oss-content".getBytes(StandardCharsets.UTF_8);

                var stored = adapter.store("/demo/test.txt", content, "test.txt", "text/plain");

                assertEquals("/demo/test.txt", stored.path());
                assertEquals("demo/test.txt", stored.fileName());
                assertNotNull(stored.storageRecordId());
                assertEquals(12345L, stored.storageRecordId());
            }
        }

        @Test
        @DisplayName("store() OSS上传失败回退本地")
        void shouldFallbackToLocalWhenOssUploadFails() {
            System.setProperty("docman.upload.localRoot", tempDir.toString());

            SysOssMapper mockMapper = mock(SysOssMapper.class);
            OssClient mockClient = mock(OssClient.class);

            try (MockedStatic<OssFactory> ossFactoryMock = mockStatic(OssFactory.class)) {
                ossFactoryMock.when(OssFactory::instance).thenReturn(mockClient);
                when(mockClient.upload(any(), anyString(), anyLong(), anyString()))
                    .thenThrow(new RuntimeException("OSS connection failed"));

                OssDocumentStorageAdapter adapter = new OssDocumentStorageAdapter(mockMapper);
                byte[] content = "fallback-content".getBytes(StandardCharsets.UTF_8);

                var stored = adapter.store("/demo/fallback.txt", content, "fallback.txt", "text/plain");

                assertEquals("/demo/fallback.txt", stored.path());
                assertEquals("fallback.txt", stored.fileName());
                assertNull(stored.storageRecordId());
                // 验证文件已写入本地
                assertTrue(Files.exists(tempDir.resolve("demo").resolve("fallback.txt")));
            }
        }

        @Test
        @DisplayName("ensureDirectory() OSS创建成功返回true")
        void shouldReturnTrueWhenOssDirectoryCreated() {
            System.setProperty("docman.upload.localRoot", tempDir.toString());

            SysOssMapper mockMapper = mock(SysOssMapper.class);
            OssClient mockClient = mock(OssClient.class);

            try (MockedStatic<OssFactory> ossFactoryMock = mockStatic(OssFactory.class)) {
                ossFactoryMock.when(OssFactory::instance).thenReturn(mockClient);

                OssDocumentStorageAdapter adapter = new OssDocumentStorageAdapter(mockMapper);

                boolean result = adapter.ensureDirectory("/demo/project");

                assertTrue(result);
            }
        }

        @Test
        @DisplayName("ensureDirectory() OSS失败回退本地")
        void shouldFallbackToLocalWhenOssDirectoryFails() {
            System.setProperty("docman.upload.localRoot", tempDir.toString());

            SysOssMapper mockMapper = mock(SysOssMapper.class);
            OssClient mockClient = mock(OssClient.class);

            try (MockedStatic<OssFactory> ossFactoryMock = mockStatic(OssFactory.class)) {
                ossFactoryMock.when(OssFactory::instance).thenReturn(mockClient);
                when(mockClient.upload(any(), anyString(), anyLong(), anyString()))
                    .thenThrow(new RuntimeException("OSS not available"));

                OssDocumentStorageAdapter adapter = new OssDocumentStorageAdapter(mockMapper);

                boolean result = adapter.ensureDirectory("/demo/project");

                assertTrue(result);
                assertTrue(Files.isDirectory(tempDir.resolve("demo").resolve("project")));
            }
        }
    }
}
