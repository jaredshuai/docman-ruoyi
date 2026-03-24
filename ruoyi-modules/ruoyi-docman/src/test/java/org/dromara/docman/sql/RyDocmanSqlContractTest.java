package org.dromara.docman.sql;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Contract test for {@code script/sql/ry_docman.sql}.
 */
@Tag("local")
class RyDocmanSqlContractTest {

    private static final Path SQL_PATH = resolvePath(
        "script/sql/ry_docman.sql",
        "../../script/sql/ry_docman.sql",
        "D:/codespace/docman-ruoyi/script/sql/ry_docman.sql"
    );

    @Test
    void docNodeDeadlineTableShouldKeepRequiredIndexes() throws IOException {
        String tableDef = extractDeadlineTableDefinition();

        assertTrue(tableDef.contains("id                  BIGINT"),
            "doc_node_deadline.id should stay BIGINT");
        assertTrue(tableDef.contains("UNIQUE KEY uk_instance_node (process_instance_id, node_code)"),
            "doc_node_deadline should keep uk_instance_node");
        assertTrue(tableDef.contains("KEY idx_project (project_id)"),
            "doc_node_deadline should keep idx_project");
        assertTrue(tableDef.contains("KEY idx_deadline (deadline)"),
            "doc_node_deadline should keep idx_deadline");
    }

    @Test
    void upgradeSqlShouldConditionallyReplaceLegacyIndex() throws IOException {
        String sql = loadSql();

        assertTrue(sql.contains("index_name = 'idx_instance_node'"),
            "upgrade SQL should check legacy idx_instance_node");
        assertTrue(sql.contains("DROP INDEX idx_instance_node"),
            "upgrade SQL should drop legacy idx_instance_node");
        assertTrue(sql.contains("index_name = 'uk_instance_node'"),
            "upgrade SQL should check target uk_instance_node");
        assertTrue(sql.contains("ADD UNIQUE KEY uk_instance_node (process_instance_id, node_code)"),
            "upgrade SQL should add uk_instance_node");
    }

    @Test
    void dashboardMenuShouldKeepExpectedBinding() throws IOException {
        String sql = loadSql();

        assertTrue(sql.contains("VALUES (3052, '仪表盘', 3000, 0, 'dashboard', 'docman/dashboard/index', 'C', 'docman:project:list'"),
            "menu 3052 should stay bound to docman/dashboard/index under parent 3000");
    }

    @Test
    void documentQueryMenuShouldStayUnderDocumentCenter() throws IOException {
        String sql = loadSql();

        assertTrue(sql.contains("VALUES (3014, '文档详情', 3010, 4, 'F', 'docman:document:query'"),
            "menu 3014 should stay under document center with docman:document:query");
    }

    @Test
    void forbiddenLegacyPermissionsShouldNotAppear() throws IOException {
        String sql = loadSql();

        assertFalse(sql.contains("docman:member:"),
            "legacy docman:member:* permissions should not reappear");
        assertFalse(sql.contains("docman:process:view"),
            "legacy docman:process:view should not reappear");
        assertFalse(sql.contains("docman:archive:view"),
            "legacy docman:archive:view should not reappear");
    }

    private static String extractDeadlineTableDefinition() throws IOException {
        String sql = loadSql();
        int start = sql.indexOf("CREATE TABLE doc_node_deadline");
        assertTrue(start >= 0, "doc_node_deadline table definition should exist");
        int end = sql.indexOf("ENGINE=InnoDB", start);
        assertTrue(end > start, "doc_node_deadline table definition should terminate with ENGINE=InnoDB");
        return sql.substring(start, end);
    }

    private static String loadSql() throws IOException {
        assertTrue(Files.exists(SQL_PATH), "SQL file not found: " + SQL_PATH);
        return Files.readString(SQL_PATH);
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
}
