package org.dromara.docman.plugin.runtime;

import org.dromara.docman.application.port.out.PluginExecutionLogPort;
import org.dromara.docman.domain.entity.DocPluginExecutionLog;
import org.dromara.docman.domain.enums.DocPluginExecutionStatus;
import org.dromara.docman.plugin.DocumentPlugin;
import org.dromara.docman.plugin.PluginContext;
import org.dromara.docman.plugin.PluginResult;
import org.dromara.docman.plugin.PluginType;
import org.junit.jupiter.api.*;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Direct unit tests for PluginExecutor without Spring context.
 */
@Tag("local")
class PluginExecutorTest {

    private DocPluginExecutionLog savedLog;
    private RuntimeException saveException;

    private PluginExecutionLogPort createLogPort() {
        return log -> {
            if (saveException != null) {
                throw saveException;
            }
            savedLog = log;
        };
    }

    private DocumentPlugin createPlugin(String id, String name, PluginResult result) {
        return new DocumentPlugin() {
            @Override public String getPluginId() { return id; }
            @Override public String getPluginName() { return name; }
            @Override public PluginType getPluginType() { return PluginType.EXCEL_FILL; }
            @Override public List<org.dromara.docman.plugin.FieldDef> getInputFields() { return List.of(); }
            @Override public List<org.dromara.docman.plugin.FieldDef> getOutputFields() { return List.of(); }
            @Override public PluginResult execute(PluginContext ctx) { return result; }
        };
    }

    private DocumentPlugin createThrowingPlugin(String id, String name, RuntimeException ex) {
        return new DocumentPlugin() {
            @Override public String getPluginId() { return id; }
            @Override public String getPluginName() { return name; }
            @Override public PluginType getPluginType() { return PluginType.EXCEL_FILL; }
            @Override public List<org.dromara.docman.plugin.FieldDef> getInputFields() { return List.of(); }
            @Override public List<org.dromara.docman.plugin.FieldDef> getOutputFields() { return List.of(); }
            @Override public PluginResult execute(PluginContext ctx) { throw ex; }
        };
    }

    private PluginContext createContext() {
        return PluginContext.builder()
            .projectId(1L)
            .projectName("test-project")
            .processInstanceId(100L)
            .nodeCode("node-1")
            .nasBasePath("/nas")
            .archiveFolderName("archive")
            .build();
    }

    @Nested
    @DisplayName("successful execution log persistence")
    class SuccessfulExecution {

        @Test
        @DisplayName("should persist log with SUCCESS status when plugin succeeds")
        void shouldPersistSuccessLog() {
            PluginExecutionLogPort logPort = createLogPort();
            PluginExecutor executor = new PluginExecutor(logPort);
            PluginResult result = PluginResult.ok();
            DocumentPlugin plugin = createPlugin("test-plugin", "Test Plugin", result);
            PluginContext context = createContext();
            PluginExecutionRequest request = PluginExecutionRequest.builder()
                .plugin(plugin)
                .context(context)
                .build();

            PluginExecutionResult executionResult = executor.execute(request);

            assertNotNull(savedLog);
            assertEquals("test-plugin", savedLog.getPluginId());
            assertEquals("Test Plugin", savedLog.getPluginName());
            assertEquals(DocPluginExecutionStatus.SUCCESS.getCode(), savedLog.getStatus());
            assertNotNull(savedLog.getRequestSnapshot());
            assertNotNull(savedLog.getResultSnapshot());
            assertEquals("test-plugin", executionResult.getPluginId());
            assertTrue(executionResult.getResult().isSuccess());
        }

        @Test
        @DisplayName("should persist log with project and process info")
        void shouldPersistProjectAndProcessInfo() {
            PluginExecutionLogPort logPort = createLogPort();
            PluginExecutor executor = new PluginExecutor(logPort);
            DocumentPlugin plugin = createPlugin("p1", "P1", PluginResult.ok());
            PluginContext context = PluginContext.builder()
                .projectId(42L)
                .processInstanceId(999L)
                .nodeCode("node-x")
                .build();
            PluginExecutionRequest request = PluginExecutionRequest.builder()
                .plugin(plugin)
                .context(context)
                .build();

            executor.execute(request);

            assertEquals(42L, savedLog.getProjectId());
            assertEquals(999L, savedLog.getProcessInstanceId());
            assertEquals("node-x", savedLog.getNodeCode());
        }
    }

    @Nested
    @DisplayName("exception to failure result conversion")
    class ExceptionToFailure {

        @Test
        @DisplayName("should convert exception to failure result")
        void shouldConvertExceptionToFailure() {
            PluginExecutionLogPort logPort = createLogPort();
            PluginExecutor executor = new PluginExecutor(logPort);
            RuntimeException ex = new RuntimeException("something went wrong");
            DocumentPlugin plugin = createThrowingPlugin("fail-plugin", "Fail Plugin", ex);
            PluginContext context = createContext();
            PluginExecutionRequest request = PluginExecutionRequest.builder()
                .plugin(plugin)
                .context(context)
                .build();

            PluginExecutionResult result = executor.execute(request);

            assertNotNull(result);
            assertEquals("fail-plugin", result.getPluginId());
            assertFalse(result.getResult().isSuccess());
            assertTrue(result.getResult().getErrorMessage().contains("something went wrong"));
        }

        @Test
        @DisplayName("should persist FAILED status when exception occurs")
        void shouldPersistFailedStatus() {
            PluginExecutionLogPort logPort = createLogPort();
            PluginExecutor executor = new PluginExecutor(logPort);
            DocumentPlugin plugin = createThrowingPlugin("err-plugin", "Err Plugin",
                new NullPointerException("null value"));
            PluginExecutionRequest request = PluginExecutionRequest.builder()
                .plugin(plugin)
                .context(createContext())
                .build();

            executor.execute(request);

            assertEquals(DocPluginExecutionStatus.FAILED.getCode(), savedLog.getStatus());
            assertTrue(savedLog.getErrorMessage().contains("null value"));
        }
    }

    @Nested
    @DisplayName("generated file count")
    class GeneratedFileCount {

        @Test
        @DisplayName("should count generated files correctly")
        void shouldCountFiles() {
            PluginExecutionLogPort logPort = createLogPort();
            PluginExecutor executor = new PluginExecutor(logPort);
            List<PluginResult.GeneratedFile> files = List.of(
                PluginResult.GeneratedFile.builder().fileName("f1.xlsx").nasPath("/a/f1.xlsx").build(),
                PluginResult.GeneratedFile.builder().fileName("f2.xlsx").nasPath("/a/f2.xlsx").build(),
                PluginResult.GeneratedFile.builder().fileName("f3.pdf").nasPath("/a/f3.pdf").build()
            );
            PluginResult result = PluginResult.ok(files);
            DocumentPlugin plugin = createPlugin("file-plugin", "File Plugin", result);
            PluginExecutionRequest request = PluginExecutionRequest.builder()
                .plugin(plugin)
                .context(createContext())
                .build();

            executor.execute(request);

            assertEquals(3, savedLog.getGeneratedFileCount());
        }

        @Test
        @DisplayName("should return zero when no files generated")
        void shouldReturnZeroForNoFiles() {
            PluginExecutionLogPort logPort = createLogPort();
            PluginExecutor executor = new PluginExecutor(logPort);
            PluginResult result = PluginResult.ok(null);
            DocumentPlugin plugin = createPlugin("no-files", "No Files", result);
            PluginExecutionRequest request = PluginExecutionRequest.builder()
                .plugin(plugin)
                .context(createContext())
                .build();

            executor.execute(request);

            assertEquals(0, savedLog.getGeneratedFileCount());
        }

        @Test
        @DisplayName("should return zero for empty file list")
        void shouldReturnZeroForEmptyList() {
            PluginExecutionLogPort logPort = createLogPort();
            PluginExecutor executor = new PluginExecutor(logPort);
            PluginResult result = PluginResult.ok(new ArrayList<>());
            DocumentPlugin plugin = createPlugin("empty-files", "Empty Files", result);
            PluginExecutionRequest request = PluginExecutionRequest.builder()
                .plugin(plugin)
                .context(createContext())
                .build();

            executor.execute(request);

            assertEquals(0, savedLog.getGeneratedFileCount());
        }
    }

    @Nested
    @DisplayName("log save failure swallowed")
    class LogSaveFailure {

        @BeforeEach
        void setUp() {
            saveException = null;
            savedLog = null;
        }

        @Test
        @DisplayName("should not throw when log save fails")
        void shouldNotThrowWhenLogSaveFails() {
            saveException = new RuntimeException("DB connection failed");
            PluginExecutionLogPort logPort = createLogPort();
            PluginExecutor executor = new PluginExecutor(logPort);
            DocumentPlugin plugin = createPlugin("swallow-test", "Swallow Test", PluginResult.ok());
            PluginExecutionRequest request = PluginExecutionRequest.builder()
                .plugin(plugin)
                .context(createContext())
                .build();

            assertDoesNotThrow(() -> executor.execute(request));
            assertNull(savedLog);
        }

        @Test
        @DisplayName("should still return result when log save fails")
        void shouldReturnResultWhenLogSaveFails() {
            saveException = new RuntimeException("DB error");
            PluginExecutionLogPort logPort = createLogPort();
            PluginExecutor executor = new PluginExecutor(logPort);
            PluginResult expected = PluginResult.ok();
            DocumentPlugin plugin = createPlugin("result-test", "Result Test", expected);
            PluginExecutionRequest request = PluginExecutionRequest.builder()
                .plugin(plugin)
                .context(createContext())
                .build();

            PluginExecutionResult result = executor.execute(request);

            assertNotNull(result);
            assertEquals("result-test", result.getPluginId());
            assertTrue(result.getResult().isSuccess());
        }
    }

    @Nested
    @DisplayName("cost time tracking")
    class CostTimeTracking {

        @Test
        @DisplayName("should record positive cost time")
        void shouldRecordPositiveCostTime() {
            PluginExecutionLogPort logPort = createLogPort();
            PluginExecutor executor = new PluginExecutor(logPort);
            DocumentPlugin plugin = createPlugin("time-plugin", "Time Plugin", PluginResult.ok());
            PluginExecutionRequest request = PluginExecutionRequest.builder()
                .plugin(plugin)
                .context(createContext())
                .build();

            PluginExecutionResult result = executor.execute(request);

            assertTrue(result.getCostMs() >= 0);
            assertTrue(savedLog.getCostMs() >= 0);
        }
    }
}
