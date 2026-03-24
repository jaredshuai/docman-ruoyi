package org.dromara.docman.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Controller permission contract test.
 */
@Tag("local")
class DocPermissionContractTest {

    private static final Pattern SQL_PERMISSION_PATTERN = Pattern.compile("'(docman:[^']+)'");
    private static final Set<Class<?>> CONTROLLERS = Set.of(
        DocArchiveController.class,
        DocDashboardController.class,
        DocDocumentRecordController.class,
        DocNodeDeadlineController.class,
        DocPluginController.class,
        DocProcessController.class,
        DocProjectController.class,
        DocProjectMemberController.class
    );

    private static final Set<String> SQL_PERMISSIONS = new HashSet<>();
    private static final Set<String> CONTROLLER_PERMISSIONS = new HashSet<>();
    private static final String[] FORBIDDEN_PREFIXES = {
        "docman:member:",
        "docman:process:view",
        "docman:archive:view"
    };

    @BeforeAll
    static void loadContracts() throws IOException {
        String sql = Files.readString(resolveSqlPath());
        Matcher matcher = SQL_PERMISSION_PATTERN.matcher(sql);
        while (matcher.find()) {
            SQL_PERMISSIONS.add(matcher.group(1));
        }

        for (Class<?> controller : CONTROLLERS) {
            for (Method method : controller.getDeclaredMethods()) {
                SaCheckPermission annotation = method.getAnnotation(SaCheckPermission.class);
                if (annotation != null) {
                    CONTROLLER_PERMISSIONS.addAll(Arrays.asList(annotation.value()));
                }
            }
        }

        assertFalse(SQL_PERMISSIONS.isEmpty(), "No docman permissions extracted from SQL");
        assertFalse(CONTROLLER_PERMISSIONS.isEmpty(), "No @SaCheckPermission values extracted from controllers");
    }

    @Test
    void everyControllerPermissionShouldExistInSql() {
        Set<String> missing = new HashSet<>(CONTROLLER_PERMISSIONS);
        missing.removeAll(SQL_PERMISSIONS);
        assertTrue(missing.isEmpty(), "Controller permissions missing in SQL: " + missing);
    }

    @Test
    void forbiddenLegacyPermissionsShouldNotAppearInSql() throws IOException {
        String sql = Files.readString(resolveSqlPath());
        for (String forbidden : FORBIDDEN_PREFIXES) {
            assertFalse(sql.contains(forbidden),
                "Forbidden legacy permission should not appear in SQL: " + forbidden);
        }
    }

    @Test
    void forbiddenLegacyPermissionsShouldNotAppearInControllers() {
        for (String permission : CONTROLLER_PERMISSIONS) {
            for (String forbidden : FORBIDDEN_PREFIXES) {
                assertFalse(permission.startsWith(forbidden),
                    "Forbidden legacy permission should not appear in controllers: " + permission);
            }
        }
    }

    private static Path resolveSqlPath() {
        for (String candidate : new String[] {
            "script/sql/ry_docman.sql",
            "../../script/sql/ry_docman.sql",
            "D:/codespace/docman-ruoyi/script/sql/ry_docman.sql"
        }) {
            Path path = Paths.get(candidate);
            if (Files.exists(path)) {
                return path;
            }
        }
        return Paths.get("script/sql/ry_docman.sql");
    }
}
