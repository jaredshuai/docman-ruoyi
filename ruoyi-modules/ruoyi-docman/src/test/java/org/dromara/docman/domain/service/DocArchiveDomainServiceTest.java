package org.dromara.docman.domain.service;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.dromara.docman.domain.entity.DocArchivePackage;
import org.dromara.docman.domain.entity.DocDocumentRecord;
import org.dromara.docman.domain.entity.DocProject;
import org.dromara.docman.domain.enums.DocArchiveStatus;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@Tag("dev")
@Tag("prod")
@Tag("local")
class DocArchiveDomainServiceTest {

    private final DocArchiveDomainService service = new DocArchiveDomainService();
    private static org.springframework.context.support.GenericApplicationContext springContext;

    @BeforeAll
    static void installMinimalSpringUtilsContext() {
        springContext = new org.springframework.context.support.GenericApplicationContext();
        springContext.registerBean(ObjectMapper.class, () -> new ObjectMapper());
        springContext.refresh();
        setSpringUtilField("beanFactory", springContext.getBeanFactory());
        setSpringUtilField("applicationContext", springContext);
    }

    @AfterAll
    static void releaseSpringUtilsContext() {
        setSpringUtilField("beanFactory", null);
        setSpringUtilField("applicationContext", null);
        if (springContext != null) {
            springContext.close();
        }
    }

    private static void setSpringUtilField(String fieldName, Object value) {
        try {
            Field field = cn.hutool.extra.spring.SpringUtil.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(null, value);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to configure Hutool SpringUtil for test", e);
        }
    }

    // ==================== createArchivePackage Field Population Tests ====================

    @Test
    void createArchivePackage_shouldPopulateProjectId() {
        DocProject project = createProject(100L, "/archive/path");
        List<Map<String, String>> manifest = createSingleEntryManifest("file.docx", "/path/file.docx");

        DocArchivePackage result = service.createArchivePackage(project, manifest, 1L);

        assertEquals(100L, result.getProjectId());
    }

    @Test
    void createArchivePackage_shouldPopulateArchiveVersion() {
        DocProject project = createProject(1L, "/path");
        List<Map<String, String>> manifest = List.of();

        DocArchivePackage result = service.createArchivePackage(project, manifest, 5L);

        assertEquals(5L, result.getArchiveVersion());
    }

    @Test
    void createArchivePackage_shouldPopulateNasArchivePath() {
        DocProject project = createProject(1L, "/archive/projects/2026");
        List<Map<String, String>> manifest = List.of();

        DocArchivePackage result = service.createArchivePackage(project, manifest, 1L);

        assertEquals("/archive/projects/2026", result.getNasArchivePath());
    }

    @Test
    void createArchivePackage_shouldPopulateManifest() {
        DocProject project = createProject(1L, "/path");
        List<Map<String, String>> manifest = createSingleEntryManifest("report.pdf", "/docs/report.pdf");

        DocArchivePackage result = service.createArchivePackage(project, manifest, 1L);

        assertEquals(manifest, result.getManifest());
        assertEquals(1, result.getManifest().size());
    }

    @Test
    void createArchivePackage_shouldPopulateStatusAsCompleted() {
        DocProject project = createProject(1L, "/path");
        List<Map<String, String>> manifest = List.of();

        DocArchivePackage result = service.createArchivePackage(project, manifest, 1L);

        assertEquals(DocArchiveStatus.COMPLETED.getCode(), result.getStatus());
    }

    @Test
    void createArchivePackage_shouldPopulateRequestedAtAndCompletedAt() {
        DocProject project = createProject(1L, "/path");
        List<Map<String, String>> manifest = List.of();

        DocArchivePackage result = service.createArchivePackage(project, manifest, 1L);

        assertNotNull(result.getRequestedAt());
        assertNotNull(result.getCompletedAt());
    }

    @Test
    void createArchivePackage_shouldSetSameTimestampForRequestedAndCompleted() {
        DocProject project = createProject(1L, "/path");
        List<Map<String, String>> manifest = List.of();

        DocArchivePackage result = service.createArchivePackage(project, manifest, 1L);

        assertEquals(result.getRequestedAt(), result.getCompletedAt());
    }

    // ==================== archiveNo Format Tests ====================

    @Test
    void archiveNo_shouldStartWithArcPrefix() {
        DocProject project = createProject(1L, "/path");
        List<Map<String, String>> manifest = List.of();

        DocArchivePackage result = service.createArchivePackage(project, manifest, 1L);

        assertTrue(result.getArchiveNo().startsWith("ARC-"));
    }

    @Test
    void archiveNo_shouldContainProjectId() {
        DocProject project = createProject(42L, "/path");
        List<Map<String, String>> manifest = List.of();

        DocArchivePackage result = service.createArchivePackage(project, manifest, 1L);

        assertTrue(result.getArchiveNo().contains("-42-"));
    }

    @Test
    void archiveNo_shouldContainVersion() {
        DocProject project = createProject(1L, "/path");
        List<Map<String, String>> manifest = List.of();

        DocArchivePackage result = service.createArchivePackage(project, manifest, 3L);

        assertTrue(result.getArchiveNo().endsWith("-V3"));
    }

    @Test
    void archiveNo_shouldMatchExpectedPattern() {
        DocProject project = createProject(100L, "/path");
        List<Map<String, String>> manifest = List.of();

        DocArchivePackage result = service.createArchivePackage(project, manifest, 2L);

        // Pattern: ARC-yyyyMMdd-projectId-Vversion
        assertTrue(result.getArchiveNo().matches("ARC-\\d{8}-\\d+-V\\d+"));
    }

    @Test
    void archiveNo_shouldContainPureDateFormat() {
        DocProject project = createProject(1L, "/path");
        List<Map<String, String>> manifest = List.of();

        DocArchivePackage result = service.createArchivePackage(project, manifest, 1L);

        // Verify date part is in PURE_DATE_PATTERN format (yyyyMMdd)
        String archiveNo = result.getArchiveNo();
        // Extract date portion: ARC-yyyyMMdd-...
        String[] parts = archiveNo.split("-");
        assertEquals(8, parts[1].length()); // yyyyMMdd has 8 digits
        assertTrue(parts[1].matches("\\d{8}"));
    }

    // ==================== Manifest Checksum Consistency Tests ====================

    @Test
    void snapshotChecksum_shouldBeConsistentForSameManifest() {
        DocProject project = createProject(1L, "/path");
        List<Map<String, String>> manifest = createSingleEntryManifest("doc.pdf", "/docs/doc.pdf");

        DocArchivePackage result1 = service.createArchivePackage(project, manifest, 1L);
        DocArchivePackage result2 = service.createArchivePackage(project, manifest, 1L);

        assertEquals(result1.getSnapshotChecksum(), result2.getSnapshotChecksum());
    }

    @Test
    void snapshotChecksum_shouldBeValidSha256Hex() {
        DocProject project = createProject(1L, "/path");
        List<Map<String, String>> manifest = createSingleEntryManifest("file.pdf", "/path/file.pdf");

        DocArchivePackage result = service.createArchivePackage(project, manifest, 1L);

        // SHA-256 produces 64 hex characters
        assertEquals(64, result.getSnapshotChecksum().length());
        assertTrue(result.getSnapshotChecksum().matches("[0-9a-f]+"));
    }

    @Test
    void snapshotChecksum_shouldDifferForDifferentManifests() {
        DocProject project = createProject(1L, "/path");
        List<Map<String, String>> manifest1 = createSingleEntryManifest("file1.pdf", "/path/file1.pdf");
        List<Map<String, String>> manifest2 = createSingleEntryManifest("file2.pdf", "/path/file2.pdf");

        DocArchivePackage result1 = service.createArchivePackage(project, manifest1, 1L);
        DocArchivePackage result2 = service.createArchivePackage(project, manifest2, 1L);

        assertNotEquals(result1.getSnapshotChecksum(), result2.getSnapshotChecksum());
    }

    @Test
    void snapshotChecksum_shouldMatchDirectSha256Computation() throws Exception {
        List<Map<String, String>> manifest = createSingleEntryManifest("test.docx", "/test.docx");

        // Compute checksum directly using ObjectMapper (same as JsonUtils uses)
        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writeValueAsString(manifest);
        String expectedChecksum = DigestUtil.sha256Hex(json);

        DocProject project = createProject(1L, "/path");
        DocArchivePackage result = service.createArchivePackage(project, manifest, 1L);

        assertEquals(expectedChecksum, result.getSnapshotChecksum());
    }

    // ==================== buildSnapshotManifest Date Formatting Tests ====================

    @Test
    void buildSnapshotManifest_shouldFormatDateInNormDatetimePattern() {
        Date testDate = DateUtil.parse("2026-03-23 14:30:45");
        List<DocDocumentRecord> records = List.of(
            createRecord("doc.pdf", "/path/doc.pdf", "plugin", "generated", testDate)
        );

        List<Map<String, String>> manifest = service.buildSnapshotManifest(records);

        assertEquals("2026-03-23 14:30:45", manifest.get(0).get("generatedAt"));
    }

    @Test
    void buildSnapshotManifest_shouldHandleNullGeneratedAt() {
        List<DocDocumentRecord> records = List.of(
            createRecord("doc.pdf", "/path/doc.pdf", "upload", "generated", null)
        );

        List<Map<String, String>> manifest = service.buildSnapshotManifest(records);

        assertEquals("", manifest.get(0).get("generatedAt"));
    }

    @Test
    void buildSnapshotManifest_shouldFormatMultipleDatesCorrectly() {
        Date date1 = DateUtil.parse("2026-01-15 09:00:00");
        Date date2 = DateUtil.parse("2026-12-31 23:59:59");
        List<DocDocumentRecord> records = List.of(
            createRecord("first.pdf", "/first.pdf", "plugin", "generated", date1),
            createRecord("second.pdf", "/second.pdf", "plugin", "generated", date2)
        );

        List<Map<String, String>> manifest = service.buildSnapshotManifest(records);

        assertEquals("2026-01-15 09:00:00", manifest.get(0).get("generatedAt"));
        assertEquals("2026-12-31 23:59:59", manifest.get(1).get("generatedAt"));
    }

    // ==================== buildSnapshotManifest Field Tests ====================

    @Test
    void buildSnapshotManifest_shouldIncludeAllRequiredFields() {
        Date date = new Date();
        List<DocDocumentRecord> records = List.of(
            createRecord("report.xlsx", "/archive/report.xlsx", "plugin", "generated", date)
        );

        List<Map<String, String>> manifest = service.buildSnapshotManifest(records);

        Map<String, String> entry = manifest.get(0);
        assertEquals("report.xlsx", entry.get("fileName"));
        assertEquals("/archive/report.xlsx", entry.get("nasPath"));
        assertEquals("plugin", entry.get("sourceType"));
        assertEquals("generated", entry.get("status"));
        assertNotNull(entry.get("generatedAt"));
    }

    @Test
    void buildSnapshotManifest_shouldReturnEmptyListForEmptyInput() {
        List<Map<String, String>> manifest = service.buildSnapshotManifest(List.of());

        assertTrue(manifest.isEmpty());
    }

    // ==================== Manifest Ordering Preservation Tests ====================

    @Test
    void buildSnapshotManifest_shouldPreserveInputOrder() {
        List<DocDocumentRecord> records = List.of(
            createRecord("first.docx", "/first.docx", "upload", "generated", new Date()),
            createRecord("second.docx", "/second.docx", "plugin", "generated", new Date()),
            createRecord("third.docx", "/third.docx", "upload", "generated", new Date())
        );

        List<Map<String, String>> manifest = service.buildSnapshotManifest(records);

        assertEquals(3, manifest.size());
        assertEquals("first.docx", manifest.get(0).get("fileName"));
        assertEquals("second.docx", manifest.get(1).get("fileName"));
        assertEquals("third.docx", manifest.get(2).get("fileName"));
    }

    @Test
    void buildSnapshotManifest_shouldUseLinkedHashMapForFieldOrder() {
        List<DocDocumentRecord> records = List.of(
            createRecord("doc.pdf", "/doc.pdf", "upload", "generated", new Date())
        );

        List<Map<String, String>> manifest = service.buildSnapshotManifest(records);

        List<String> expectedOrder = List.of("fileName", "nasPath", "sourceType", "status", "generatedAt");
        List<String> actualOrder = new ArrayList<>(manifest.get(0).keySet());

        assertEquals(expectedOrder, actualOrder);
    }

    @Test
    void manifest_shouldMaintainSameFieldOrderAcrossEntries() {
        List<DocDocumentRecord> records = List.of(
            createRecord("a.pdf", "/a.pdf", "plugin", "generated", new Date()),
            createRecord("b.pdf", "/b.pdf", "upload", "generated", null)
        );

        List<Map<String, String>> manifest = service.buildSnapshotManifest(records);

        // Verify both entries have same key order
        List<String> firstKeys = new ArrayList<>(manifest.get(0).keySet());
        List<String> secondKeys = new ArrayList<>(manifest.get(1).keySet());
        assertEquals(firstKeys, secondKeys);
    }

    // ==================== Helper Methods ====================

    private DocProject createProject(Long id, String nasBasePath) {
        DocProject project = new DocProject();
        project.setId(id);
        project.setNasBasePath(nasBasePath);
        return project;
    }

    private DocDocumentRecord createRecord(String fileName, String nasPath, String sourceType, String status, Date generatedAt) {
        DocDocumentRecord record = new DocDocumentRecord();
        record.setFileName(fileName);
        record.setNasPath(nasPath);
        record.setSourceType(sourceType);
        record.setStatus(status);
        record.setGeneratedAt(generatedAt);
        return record;
    }

    private List<Map<String, String>> createSingleEntryManifest(String fileName, String nasPath) {
        Map<String, String> entry = new LinkedHashMap<>();
        entry.put("fileName", fileName);
        entry.put("nasPath", nasPath);
        entry.put("sourceType", "plugin");
        entry.put("status", "generated");
        entry.put("generatedAt", "2026-03-23 10:00:00");
        return new ArrayList<>(List.of(entry));
    }
}
