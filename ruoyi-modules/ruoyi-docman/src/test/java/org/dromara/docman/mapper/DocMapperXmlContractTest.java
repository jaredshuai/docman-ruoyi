package org.dromara.docman.mapper;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Contract tests for MyBatis mapper XML files in ruoyi-docman module.
 * Validates XML structure, namespace correctness, and SQL safety.
 */
@Tag("dev")
@Tag("prod")
@Tag("local")
class DocMapperXmlContractTest {

    private static final Path MAPPER_XML_DIR = resolvePath(
        "src/main/resources/mapper/docman",
        "ruoyi-modules/ruoyi-docman/src/main/resources/mapper/docman"
    );
    private static final Path MAPPER_JAVA_DIR = resolvePath(
        "src/main/java/org/dromara/docman/mapper",
        "ruoyi-modules/ruoyi-docman/src/main/java/org/dromara/docman/mapper"
    );

    private static final Pattern DOLLAR_PLACEHOLDER = Pattern.compile("\\$\\{[^}]+\\}");
    private static final Set<String> ALLOWED_DOLLAR_PLACEHOLDERS = Set.of("${ew.customSqlSegment}");

    private static final List<File> XML_FILES = loadXmlFiles();
    private static final Map<String, File> XML_BY_NAMESPACE = buildNamespaceMap();

    static {
        assertFalse(XML_FILES.isEmpty(), "No mapper XML files found under " + MAPPER_XML_DIR);
    }

    private static List<File> loadXmlFiles() {
        List<File> xmlFiles = new ArrayList<>();
        File dir = MAPPER_XML_DIR.toFile();
        if (dir.exists() && dir.isDirectory()) {
            File[] files = dir.listFiles((d, name) -> name.endsWith(".xml"));
            if (files != null) {
                for (File f : files) {
                    xmlFiles.add(f);
                }
            }
        }
        return xmlFiles;
    }

    private static Map<String, File> buildNamespaceMap() {
        Map<String, File> xmlByNamespace = new HashMap<>();
        for (File xml : XML_FILES) {
            String ns = extractNamespace(xml);
            if (ns != null) {
                xmlByNamespace.put(ns, xml);
            }
        }
        return xmlByNamespace;
    }

    @Nested
    @DisplayName("Namespace validation")
    class NamespaceValidation {

        @Test
        @DisplayName("Each XML namespace should match its corresponding mapper interface FQCN")
        void namespaceShouldMatchMapperInterfaceFQCN() throws Exception {
            for (File xml : XML_FILES) {
                String xmlName = xml.getName();
                String expectedInterfaceName = xmlName.replace(".xml", "");
                Path interfacePath = MAPPER_JAVA_DIR.resolve(expectedInterfaceName + ".java");

                assertTrue(interfacePath.toFile().exists(),
                    "Mapper interface should exist: " + interfacePath);

                String namespace = extractNamespace(xml);
                assertNotNull(namespace, "Namespace should not be null for " + xmlName);

                String expectedFQCN = "org.dromara.docman.mapper." + expectedInterfaceName;
                assertEquals(expectedFQCN, namespace,
                    "Namespace in " + xmlName + " should match interface FQCN");
            }
        }
    }

    @Nested
    @DisplayName("SQL injection safety")
    class SqlInjectionSafety {

        @Test
        @DisplayName("No XML should contain ${} placeholders (use #{} instead)")
        void noDollarPlaceholders() throws Exception {
            for (File xml : XML_FILES) {
                String content = Files.readString(xml.toPath());

                List<String> violations = new ArrayList<>();
                Matcher m = DOLLAR_PLACEHOLDER.matcher(content);
                while (m.find()) {
                    String placeholder = m.group();
                    if (!ALLOWED_DOLLAR_PLACEHOLDERS.contains(placeholder)) {
                        violations.add(placeholder);
                    }
                }

                assertTrue(violations.isEmpty(),
                    xml.getName() + " should not contain ${} placeholders (SQL injection risk). Found: " + violations);
            }
        }
    }

    @Nested
    @DisplayName("Foreach element validation")
    class ForeachValidation {

        @Test
        @DisplayName("Every foreach element should have required attributes")
        void foreachHasRequiredAttributes() throws Exception {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(false);
            DocumentBuilder builder = factory.newDocumentBuilder();

            for (File xml : XML_FILES) {
                Document doc = builder.parse(xml);
                NodeList foreachNodes = doc.getElementsByTagName("foreach");

                for (int i = 0; i < foreachNodes.getLength(); i++) {
                    Element foreach = (Element) foreachNodes.item(i);
                    String errorMsg = xml.getName() + " foreach[" + i + "] missing required attribute: ";

                    assertTrue(foreach.hasAttribute("collection"),
                        errorMsg + "collection");
                    assertTrue(foreach.hasAttribute("item"),
                        errorMsg + "item");
                    assertTrue(foreach.hasAttribute("open"),
                        errorMsg + "open");
                    assertTrue(foreach.hasAttribute("close"),
                        errorMsg + "close");
                    assertTrue(foreach.hasAttribute("separator"),
                        errorMsg + "separator");
                }
            }
        }
    }

    @Nested
    @DisplayName("DocDashboardMapper specific contracts")
    class DocDashboardMapperContracts {

        private File dashboardXml() {
            File xml = XML_BY_NAMESPACE.get("org.dromara.docman.mapper.DocDashboardMapper");
            assertNotNull(xml, "DocDashboardMapper.xml should exist");
            return xml;
        }

        @Test
        @DisplayName("Should contain selectProjectProgress statement")
        void shouldContainSelectProjectProgress() throws Exception {
            assertContainsSelectId(dashboardXml(), "selectProjectProgress");
        }

        @Test
        @DisplayName("Should contain selectDeadlineAlerts statement")
        void shouldContainSelectDeadlineAlerts() throws Exception {
            assertContainsSelectId(dashboardXml(), "selectDeadlineAlerts");
        }

        @Test
        @DisplayName("Should contain selectPluginStats statement")
        void shouldContainSelectPluginStats() throws Exception {
            assertContainsSelectId(dashboardXml(), "selectPluginStats");
        }

        @Test
        @DisplayName("selectProjectProgress should query doc_project table")
        void selectProjectProgressShouldQueryDocProject() throws Exception {
            String content = Files.readString(dashboardXml().toPath());
            assertTrue(content.contains("from doc_project p"),
                "selectProjectProgress should query doc_project table with alias 'p'");
        }

        @Test
        @DisplayName("selectDeadlineAlerts should query doc_node_deadline table")
        void selectDeadlineAlertsShouldQueryDocNodeDeadline() throws Exception {
            String content = Files.readString(dashboardXml().toPath());
            assertTrue(content.contains("from doc_node_deadline d"),
                "selectDeadlineAlerts should query doc_node_deadline table with alias 'd'");
        }

        @Test
        @DisplayName("selectPluginStats should query doc_plugin_execution_log table")
        void selectPluginStatsShouldQueryDocPluginExecutionLog() throws Exception {
            String content = Files.readString(dashboardXml().toPath());
            assertTrue(content.contains("from doc_plugin_execution_log l"),
                "selectPluginStats should query doc_plugin_execution_log table with alias 'l'");
        }

        @Test
        @DisplayName("All select statements should use #{} placeholders")
        void allSelectsUseSafePlaceholders() throws Exception {
            String content = Files.readString(dashboardXml().toPath());
            // Verify each select uses #{projectId} or #{today} etc.
            assertTrue(content.contains("#{projectId}"),
                "Dashboard queries should use #{projectId} placeholder");
            assertTrue(content.contains("#{today}"),
                "selectDeadlineAlerts should use #{today} placeholder");
            assertTrue(content.contains("n.node_code = d.node_code"),
                "selectDeadlineAlerts should join flow_node by node_code");
            assertTrue(content.contains("n.definition_id = pc.definition_id"),
                "selectDeadlineAlerts should join flow_node by definition_id");
            assertTrue(content.contains("order by totalCount desc"),
                "selectPluginStats should order by totalCount desc");
        }
    }

    @Nested
    @DisplayName("DocNodeDeadlineMapper specific contracts")
    class DocNodeDeadlineMapperContracts {

        private File deadlineXml() {
            File xml = XML_BY_NAMESPACE.get("org.dromara.docman.mapper.DocNodeDeadlineMapper");
            assertNotNull(xml, "DocNodeDeadlineMapper.xml should exist");
            return xml;
        }

        @Test
        @DisplayName("Should contain selectDeadlineList statement")
        void shouldContainSelectDeadlineList() throws Exception {
            assertContainsSelectId(deadlineXml(), "selectDeadlineList");
        }

        @Test
        @DisplayName("Should contain selectApproachingDeadlines statement")
        void shouldContainSelectApproachingDeadlines() throws Exception {
            assertContainsSelectId(deadlineXml(), "selectApproachingDeadlines");
        }

        @Test
        @DisplayName("selectDeadlineList should query doc_node_deadline table")
        void selectDeadlineListShouldQueryDocNodeDeadline() throws Exception {
            String content = Files.readString(deadlineXml().toPath());
            assertTrue(content.contains("from doc_node_deadline d"),
                "selectDeadlineList should query doc_node_deadline table with alias 'd'");
        }

        @Test
        @DisplayName("selectApproachingDeadlines should filter by deadline and reminder_count")
        void selectApproachingDeadlinesShouldFilterByDeadlineAndReminderCount() throws Exception {
            String content = Files.readString(deadlineXml().toPath());
            assertTrue(content.contains("d.deadline") && content.contains("#{deadlineBefore}"),
                "selectApproachingDeadlines should filter by deadline parameter");
            assertTrue(content.contains("d.reminder_count") && content.contains("#{maxReminderCount}"),
                "selectApproachingDeadlines should filter by reminder_count parameter");
        }

        @Test
        @DisplayName("All select statements should use #{} placeholders")
        void allSelectsUseSafePlaceholders() throws Exception {
            String content = Files.readString(deadlineXml().toPath());
            assertTrue(content.contains("#{projectId}"),
                "selectDeadlineList should use #{projectId} placeholder");
            assertTrue(content.contains("#{deadlineBefore}"),
                "selectApproachingDeadlines should use #{deadlineBefore} placeholder");
            assertTrue(content.contains("#{maxReminderCount}"),
                "selectApproachingDeadlines should use #{maxReminderCount} placeholder");
            assertTrue(content.contains("order by d.deadline asc, d.id desc"),
                "selectDeadlineList should keep the current deadline ordering");
        }
    }

    @Nested
    @DisplayName("Mapper file completeness")
    class MapperFileCompleteness {

        @Test
        @DisplayName("Expected XML-backed mappers should stay present")
        void expectedXmlBackedMappersShouldStayPresent() {
            Set<String> expectedXmlFiles = Set.of(
                "DocArchivePackageMapper.xml",
                "DocDashboardMapper.xml",
                "DocDocumentRecordMapper.xml",
                "DocNodeContextMapper.xml",
                "DocNodeDeadlineMapper.xml",
                "DocProcessConfigMapper.xml",
                "DocProjectAddRecordDetailMapper.xml",
                "DocProjectAddRecordMapper.xml",
                "DocProjectOrderMapper.xml",
                "DocProjectMapper.xml",
                "DocProjectMemberMapper.xml"
            );

            Set<String> actualXmlFiles = new HashSet<>();
            for (File xml : XML_FILES) {
                actualXmlFiles.add(xml.getName());
            }

            assertEquals(expectedXmlFiles, actualXmlFiles,
                "XML-backed mapper file set changed unexpectedly");
        }
    }

    // Helper methods

    private static String extractNamespace(File xml) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(false);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(xml);
            Element mapper = doc.getDocumentElement();
            if ("mapper".equals(mapper.getTagName())) {
                return mapper.getAttribute("namespace");
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }

    private static Path resolvePath(String... candidates) {
        for (String candidate : candidates) {
            Path path = Paths.get(candidate);
            if (Files.exists(path)) {
                return path;
            }
        }
        return Paths.get(candidates[0]);
    }

    private static void assertContainsSelectId(File xml, String selectId) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(xml);
        NodeList selectNodes = doc.getElementsByTagName("select");

        Set<String> selectIds = new HashSet<>();
        for (int i = 0; i < selectNodes.getLength(); i++) {
            Element select = (Element) selectNodes.item(i);
            selectIds.add(select.getAttribute("id"));
        }

        assertTrue(selectIds.contains(selectId),
            xml.getName() + " should contain select statement with id '" + selectId + "'. Found: " + selectIds);
    }
}
