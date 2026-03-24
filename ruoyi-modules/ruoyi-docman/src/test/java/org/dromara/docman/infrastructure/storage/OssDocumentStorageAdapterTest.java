package org.dromara.docman.infrastructure.storage;

import org.dromara.system.mapper.SysOssMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

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
}
