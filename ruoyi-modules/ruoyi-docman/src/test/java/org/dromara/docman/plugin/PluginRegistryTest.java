package org.dromara.docman.plugin;

import org.dromara.docman.plugin.annotation.DocPlugin;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Direct unit tests for PluginRegistry without Spring context.
 */
@Tag("local")
class PluginRegistryTest {

    private static DocumentPlugin createMockPlugin(String id, String name, PluginType type) {
        return new DocumentPlugin() {
            @Override public String getPluginId() { return id; }
            @Override public String getPluginName() { return name; }
            @Override public PluginType getPluginType() { return type; }
            @Override public List<FieldDef> getInputFields() { return List.of(); }
            @Override public List<FieldDef> getOutputFields() { return List.of(); }
            @Override public PluginResult execute(PluginContext ctx) { return null; }
        };
    }

    @DocPlugin("annotated-plugin")
    private static class AnnotatedPlugin implements DocumentPlugin {
        @Override public String getPluginId() { return "fallback-id"; }
        @Override public String getPluginName() { return "Annotated Plugin"; }
        @Override public PluginType getPluginType() { return PluginType.EXCEL_FILL; }
        @Override public List<FieldDef> getInputFields() { return List.of(); }
        @Override public List<FieldDef> getOutputFields() { return List.of(); }
        @Override public PluginResult execute(PluginContext ctx) { return null; }
    }

    private static class NonAnnotatedPlugin implements DocumentPlugin {
        private final String id;
        private final PluginType type;
        NonAnnotatedPlugin(String id, PluginType type) { this.id = id; this.type = type; }
        @Override public String getPluginId() { return id; }
        @Override public String getPluginName() { return "NonAnnotated " + id; }
        @Override public PluginType getPluginType() { return type; }
        @Override public List<FieldDef> getInputFields() { return List.of(); }
        @Override public List<FieldDef> getOutputFields() { return List.of(); }
        @Override public PluginResult execute(PluginContext ctx) { return null; }
    }

    @Nested
    @DisplayName("init() registration")
    class InitRegistration {

        @Test
        @DisplayName("should register plugins with annotation")
        void shouldRegisterAnnotatedPlugins() {
            List<DocumentPlugin> plugins = List.of(new AnnotatedPlugin());
            PluginRegistry registry = new PluginRegistry(plugins);

            registry.init();

            assertNotNull(registry.getPlugin("annotated-plugin"));
            assertEquals("Annotated Plugin", registry.getPlugin("annotated-plugin").getPluginName());
        }

        @Test
        @DisplayName("should register plugins without annotation using getPluginId")
        void shouldRegisterNonAnnotatedPlugins() {
            List<DocumentPlugin> plugins = List.of(new NonAnnotatedPlugin("my-id", PluginType.EXCEL_FILL));
            PluginRegistry registry = new PluginRegistry(plugins);

            registry.init();

            assertNotNull(registry.getPlugin("my-id"));
            assertEquals("NonAnnotated my-id", registry.getPlugin("my-id").getPluginName());
        }

        @Test
        @DisplayName("should handle empty plugin list")
        void shouldHandleEmptyList() {
            PluginRegistry registry = new PluginRegistry(Collections.emptyList());

            registry.init();

            assertTrue(registry.getAllPlugins().isEmpty());
        }

        @Test
        @DisplayName("should handle null plugin list")
        void shouldHandleNullList() {
            PluginRegistry registry = new PluginRegistry(null);

            registry.init();

            assertTrue(registry.getAllPlugins().isEmpty());
        }

        @Test
        @DisplayName("should register multiple plugins")
        void shouldRegisterMultiplePlugins() {
            List<DocumentPlugin> plugins = List.of(
                createMockPlugin("p1", "Plugin1", PluginType.EXCEL_FILL),
                createMockPlugin("p2", "Plugin2", PluginType.DATA_EXTRACT),
                createMockPlugin("p3", "Plugin3", PluginType.AI_GENERATE)
            );
            PluginRegistry registry = new PluginRegistry(plugins);

            registry.init();

            assertEquals(3, registry.getAllPlugins().size());
            assertNotNull(registry.getPlugin("p1"));
            assertNotNull(registry.getPlugin("p2"));
            assertNotNull(registry.getPlugin("p3"));
        }
    }

    @Nested
    @DisplayName("duplicate pluginId overwrite behavior")
    class DuplicatePluginId {

        @Test
        @DisplayName("should overwrite when duplicate pluginId exists")
        void shouldOverwriteDuplicateId() {
            DocumentPlugin first = createMockPlugin("dup-id", "First Plugin", PluginType.EXCEL_FILL);
            DocumentPlugin second = createMockPlugin("dup-id", "Second Plugin", PluginType.DATA_EXTRACT);
            List<DocumentPlugin> plugins = List.of(first, second);
            PluginRegistry registry = new PluginRegistry(plugins);

            registry.init();

            assertEquals(1, registry.getAllPlugins().size());
            assertEquals("Second Plugin", registry.getPlugin("dup-id").getPluginName());
            assertEquals(PluginType.DATA_EXTRACT, registry.getPlugin("dup-id").getPluginType());
        }

        @Test
        @DisplayName("should preserve insertion order (last wins for duplicate)")
        void shouldPreserveInsertionOrder() {
            List<DocumentPlugin> plugins = List.of(
                createMockPlugin("a", "A", PluginType.EXCEL_FILL),
                createMockPlugin("b", "B", PluginType.DATA_EXTRACT),
                createMockPlugin("a", "A2", PluginType.AI_GENERATE)
            );
            PluginRegistry registry = new PluginRegistry(plugins);

            registry.init();

            Map<String, DocumentPlugin> all = registry.getAllPlugins();
            Iterator<String> keys = all.keySet().iterator();
            assertEquals("a", keys.next());
            assertEquals("b", keys.next());
            assertFalse(keys.hasNext());
        }
    }

    @Nested
    @DisplayName("getPlugin()")
    class GetPlugin {

        @Test
        @DisplayName("should return plugin by id")
        void shouldReturnPluginById() {
            List<DocumentPlugin> plugins = List.of(createMockPlugin("test-id", "Test", PluginType.EXCEL_FILL));
            PluginRegistry registry = new PluginRegistry(plugins);
            registry.init();

            DocumentPlugin plugin = registry.getPlugin("test-id");

            assertNotNull(plugin);
            assertEquals("test-id", plugin.getPluginId());
        }

        @Test
        @DisplayName("should return null for non-existent plugin")
        void shouldReturnNullForNonExistent() {
            PluginRegistry registry = new PluginRegistry(Collections.emptyList());
            registry.init();

            assertNull(registry.getPlugin("non-existent"));
        }
    }

    @Nested
    @DisplayName("getAllPlugins() unmodifiable")
    class GetAllPlugins {

        @Test
        @DisplayName("should return unmodifiable map")
        void shouldReturnUnmodifiableMap() {
            List<DocumentPlugin> plugins = List.of(createMockPlugin("p1", "P1", PluginType.EXCEL_FILL));
            PluginRegistry registry = new PluginRegistry(plugins);
            registry.init();

            Map<String, DocumentPlugin> all = registry.getAllPlugins();

            assertThrows(UnsupportedOperationException.class, () -> all.put("new", createMockPlugin("new", "New", PluginType.EXCEL_FILL)));
            assertThrows(UnsupportedOperationException.class, () -> all.remove("p1"));
        }

        @Test
        @DisplayName("should reflect all registered plugins")
        void shouldReflectAllPlugins() {
            List<DocumentPlugin> plugins = List.of(
                createMockPlugin("id1", "Name1", PluginType.EXCEL_FILL),
                createMockPlugin("id2", "Name2", PluginType.DATA_EXTRACT)
            );
            PluginRegistry registry = new PluginRegistry(plugins);
            registry.init();

            Map<String, DocumentPlugin> all = registry.getAllPlugins();

            assertEquals(2, all.size());
            assertTrue(all.containsKey("id1"));
            assertTrue(all.containsKey("id2"));
        }
    }

    @Nested
    @DisplayName("getPluginsByType() filter")
    class GetPluginsByType {

        @Test
        @DisplayName("should filter plugins by type")
        void shouldFilterByType() {
            List<DocumentPlugin> plugins = List.of(
                createMockPlugin("p1", "P1", PluginType.EXCEL_FILL),
                createMockPlugin("p2", "P2", PluginType.DATA_EXTRACT),
                createMockPlugin("p3", "P3", PluginType.EXCEL_FILL),
                createMockPlugin("p4", "P4", PluginType.AI_GENERATE)
            );
            PluginRegistry registry = new PluginRegistry(plugins);
            registry.init();

            List<DocumentPlugin> excelPlugins = registry.getPluginsByType(PluginType.EXCEL_FILL);

            assertEquals(2, excelPlugins.size());
            assertTrue(excelPlugins.stream().allMatch(p -> p.getPluginType() == PluginType.EXCEL_FILL));
        }

        @Test
        @DisplayName("should return empty list when no plugins match type")
        void shouldReturnEmptyWhenNoMatch() {
            List<DocumentPlugin> plugins = List.of(
                createMockPlugin("p1", "P1", PluginType.EXCEL_FILL),
                createMockPlugin("p2", "P2", PluginType.DATA_EXTRACT)
            );
            PluginRegistry registry = new PluginRegistry(plugins);
            registry.init();

            List<DocumentPlugin> aiPlugins = registry.getPluginsByType(PluginType.AI_GENERATE);

            assertTrue(aiPlugins.isEmpty());
        }

        @Test
        @DisplayName("should return immutable list")
        void shouldReturnImmutableList() {
            List<DocumentPlugin> plugins = List.of(createMockPlugin("p1", "P1", PluginType.EXCEL_FILL));
            PluginRegistry registry = new PluginRegistry(plugins);
            registry.init();

            List<DocumentPlugin> result = registry.getPluginsByType(PluginType.EXCEL_FILL);

            assertThrows(UnsupportedOperationException.class, () -> result.add(createMockPlugin("p2", "P2", PluginType.EXCEL_FILL)));
        }
    }
}