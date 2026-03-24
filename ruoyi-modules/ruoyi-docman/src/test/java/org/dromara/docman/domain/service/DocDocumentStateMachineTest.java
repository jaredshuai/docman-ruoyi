package org.dromara.docman.domain.service;

import org.dromara.common.core.exception.ServiceException;
import org.dromara.docman.domain.enums.DocDocumentStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DocDocumentStateMachine 单元测试。
 * 覆盖合法/非法状态转换、canArchive 行为、终态判断、allowedTargets 验证。
 */
@Tag("dev")
@Tag("prod")
@Tag("local")
class DocDocumentStateMachineTest {

    private final DocDocumentStateMachine stateMachine = DocDocumentStateMachine.INSTANCE;

    @Nested
    @DisplayName("合法状态转换测试")
    class LegalTransitionTests {

        @Test
        @DisplayName("PENDING → RUNNING")
        void pendingToRunning() {
            assertDoesNotThrow(() -> stateMachine.ensureTransition(DocDocumentStatus.PENDING, DocDocumentStatus.RUNNING));
            assertTrue(stateMachine.canTransition(DocDocumentStatus.PENDING, DocDocumentStatus.RUNNING));
        }

        @Test
        @DisplayName("PENDING → GENERATED")
        void pendingToGenerated() {
            assertDoesNotThrow(() -> stateMachine.ensureTransition(DocDocumentStatus.PENDING, DocDocumentStatus.GENERATED));
            assertTrue(stateMachine.canTransition(DocDocumentStatus.PENDING, DocDocumentStatus.GENERATED));
        }

        @Test
        @DisplayName("PENDING → FAILED")
        void pendingToFailed() {
            assertDoesNotThrow(() -> stateMachine.ensureTransition(DocDocumentStatus.PENDING, DocDocumentStatus.FAILED));
            assertTrue(stateMachine.canTransition(DocDocumentStatus.PENDING, DocDocumentStatus.FAILED));
        }

        @Test
        @DisplayName("PENDING → OBSOLETE")
        void pendingToObsolete() {
            assertDoesNotThrow(() -> stateMachine.ensureTransition(DocDocumentStatus.PENDING, DocDocumentStatus.OBSOLETE));
            assertTrue(stateMachine.canTransition(DocDocumentStatus.PENDING, DocDocumentStatus.OBSOLETE));
        }

        @Test
        @DisplayName("RUNNING → GENERATED")
        void runningToGenerated() {
            assertDoesNotThrow(() -> stateMachine.ensureTransition(DocDocumentStatus.RUNNING, DocDocumentStatus.GENERATED));
            assertTrue(stateMachine.canTransition(DocDocumentStatus.RUNNING, DocDocumentStatus.GENERATED));
        }

        @Test
        @DisplayName("RUNNING → FAILED")
        void runningToFailed() {
            assertDoesNotThrow(() -> stateMachine.ensureTransition(DocDocumentStatus.RUNNING, DocDocumentStatus.FAILED));
            assertTrue(stateMachine.canTransition(DocDocumentStatus.RUNNING, DocDocumentStatus.FAILED));
        }

        @Test
        @DisplayName("RUNNING → OBSOLETE")
        void runningToObsolete() {
            assertDoesNotThrow(() -> stateMachine.ensureTransition(DocDocumentStatus.RUNNING, DocDocumentStatus.OBSOLETE));
            assertTrue(stateMachine.canTransition(DocDocumentStatus.RUNNING, DocDocumentStatus.OBSOLETE));
        }

        @Test
        @DisplayName("GENERATED → ARCHIVED")
        void generatedToArchived() {
            assertDoesNotThrow(() -> stateMachine.ensureTransition(DocDocumentStatus.GENERATED, DocDocumentStatus.ARCHIVED));
            assertTrue(stateMachine.canTransition(DocDocumentStatus.GENERATED, DocDocumentStatus.ARCHIVED));
        }

        @Test
        @DisplayName("GENERATED → OBSOLETE")
        void generatedToObsolete() {
            assertDoesNotThrow(() -> stateMachine.ensureTransition(DocDocumentStatus.GENERATED, DocDocumentStatus.OBSOLETE));
            assertTrue(stateMachine.canTransition(DocDocumentStatus.GENERATED, DocDocumentStatus.OBSOLETE));
        }

        @Test
        @DisplayName("FAILED → PENDING")
        void failedToPending() {
            assertDoesNotThrow(() -> stateMachine.ensureTransition(DocDocumentStatus.FAILED, DocDocumentStatus.PENDING));
            assertTrue(stateMachine.canTransition(DocDocumentStatus.FAILED, DocDocumentStatus.PENDING));
        }

        @Test
        @DisplayName("FAILED → RUNNING")
        void failedToRunning() {
            assertDoesNotThrow(() -> stateMachine.ensureTransition(DocDocumentStatus.FAILED, DocDocumentStatus.RUNNING));
            assertTrue(stateMachine.canTransition(DocDocumentStatus.FAILED, DocDocumentStatus.RUNNING));
        }

        @Test
        @DisplayName("FAILED → OBSOLETE")
        void failedToObsolete() {
            assertDoesNotThrow(() -> stateMachine.ensureTransition(DocDocumentStatus.FAILED, DocDocumentStatus.OBSOLETE));
            assertTrue(stateMachine.canTransition(DocDocumentStatus.FAILED, DocDocumentStatus.OBSOLETE));
        }

        @Test
        @DisplayName("OBSOLETE → PENDING")
        void obsoleteToPending() {
            assertDoesNotThrow(() -> stateMachine.ensureTransition(DocDocumentStatus.OBSOLETE, DocDocumentStatus.PENDING));
            assertTrue(stateMachine.canTransition(DocDocumentStatus.OBSOLETE, DocDocumentStatus.PENDING));
        }

        @Test
        @DisplayName("OBSOLETE → RUNNING")
        void obsoleteToRunning() {
            assertDoesNotThrow(() -> stateMachine.ensureTransition(DocDocumentStatus.OBSOLETE, DocDocumentStatus.RUNNING));
            assertTrue(stateMachine.canTransition(DocDocumentStatus.OBSOLETE, DocDocumentStatus.RUNNING));
        }

        @Test
        @DisplayName("状态自我转换始终合法")
        void selfTransitionIsAlwaysLegal() {
            for (DocDocumentStatus status : DocDocumentStatus.values()) {
                assertDoesNotThrow(() -> stateMachine.ensureTransition(status, status),
                    "Self transition should be legal for " + status);
                assertTrue(stateMachine.canTransition(status, status),
                    "canTransition should return true for self transition on " + status);
            }
        }
    }

    @Nested
    @DisplayName("非法状态转换测试")
    class IllegalTransitionTests {

        @Test
        @DisplayName("PENDING → ARCHIVED 非法")
        void pendingToArchivedThrows() {
            ServiceException ex = assertThrows(ServiceException.class,
                () -> stateMachine.ensureTransition(DocDocumentStatus.PENDING, DocDocumentStatus.ARCHIVED));
            assertTrue(ex.getMessage().contains("非法状态变更"));
            assertFalse(stateMachine.canTransition(DocDocumentStatus.PENDING, DocDocumentStatus.ARCHIVED));
        }

        @Test
        @DisplayName("RUNNING → PENDING 非法")
        void runningToPendingThrows() {
            assertThrows(ServiceException.class,
                () -> stateMachine.ensureTransition(DocDocumentStatus.RUNNING, DocDocumentStatus.PENDING));
            assertFalse(stateMachine.canTransition(DocDocumentStatus.RUNNING, DocDocumentStatus.PENDING));
        }

        @Test
        @DisplayName("GENERATED → PENDING 非法")
        void generatedToPendingThrows() {
            assertThrows(ServiceException.class,
                () -> stateMachine.ensureTransition(DocDocumentStatus.GENERATED, DocDocumentStatus.PENDING));
            assertFalse(stateMachine.canTransition(DocDocumentStatus.GENERATED, DocDocumentStatus.PENDING));
        }

        @Test
        @DisplayName("GENERATED → RUNNING 非法")
        void generatedToRunningThrows() {
            assertThrows(ServiceException.class,
                () -> stateMachine.ensureTransition(DocDocumentStatus.GENERATED, DocDocumentStatus.RUNNING));
            assertFalse(stateMachine.canTransition(DocDocumentStatus.GENERATED, DocDocumentStatus.RUNNING));
        }

        @Test
        @DisplayName("GENERATED → FAILED 非法")
        void generatedToFailedThrows() {
            assertThrows(ServiceException.class,
                () -> stateMachine.ensureTransition(DocDocumentStatus.GENERATED, DocDocumentStatus.FAILED));
            assertFalse(stateMachine.canTransition(DocDocumentStatus.GENERATED, DocDocumentStatus.FAILED));
        }

        @Test
        @DisplayName("ARCHIVED → 任何状态都非法")
        void archivedToAnyThrows() {
            for (DocDocumentStatus target : DocDocumentStatus.values()) {
                if (target != DocDocumentStatus.ARCHIVED) {
                    assertThrows(ServiceException.class,
                        () -> stateMachine.ensureTransition(DocDocumentStatus.ARCHIVED, target),
                        "ARCHIVED -> " + target + " should be illegal");
                    assertFalse(stateMachine.canTransition(DocDocumentStatus.ARCHIVED, target));
                }
            }
        }

        @Test
        @DisplayName("OBSOLETE → GENERATED 非法")
        void obsoleteToGeneratedThrows() {
            assertThrows(ServiceException.class,
                () -> stateMachine.ensureTransition(DocDocumentStatus.OBSOLETE, DocDocumentStatus.GENERATED));
            assertFalse(stateMachine.canTransition(DocDocumentStatus.OBSOLETE, DocDocumentStatus.GENERATED));
        }

        @Test
        @DisplayName("OBSOLETE → FAILED 非法")
        void obsoleteToFailedThrows() {
            assertThrows(ServiceException.class,
                () -> stateMachine.ensureTransition(DocDocumentStatus.OBSOLETE, DocDocumentStatus.FAILED));
            assertFalse(stateMachine.canTransition(DocDocumentStatus.OBSOLETE, DocDocumentStatus.FAILED));
        }

        @Test
        @DisplayName("OBSOLETE → ARCHIVED 非法")
        void obsoleteToArchivedThrows() {
            assertThrows(ServiceException.class,
                () -> stateMachine.ensureTransition(DocDocumentStatus.OBSOLETE, DocDocumentStatus.ARCHIVED));
            assertFalse(stateMachine.canTransition(DocDocumentStatus.OBSOLETE, DocDocumentStatus.ARCHIVED));
        }
    }

    @Nested
    @DisplayName("canArchive 行为测试")
    class CanArchiveTests {

        @Test
        @DisplayName("GENERATED 状态可以归档")
        void generatedCanArchive() {
            assertTrue(DocDocumentStateMachine.canArchive(DocDocumentStatus.GENERATED));
        }

        @ParameterizedTest
        @EnumSource(value = DocDocumentStatus.class, mode = EnumSource.Mode.EXCLUDE, names = {"GENERATED"})
        @DisplayName("非 GENERATED 状态不能归档")
        void nonGeneratedCannotArchive(DocDocumentStatus status) {
            assertFalse(DocDocumentStateMachine.canArchive(status));
        }
    }

    @Nested
    @DisplayName("终态测试")
    class TerminalStateTests {

        @Test
        @DisplayName("ARCHIVED 是终态")
        void archivedIsTerminal() {
            assertTrue(stateMachine.isTerminal(DocDocumentStatus.ARCHIVED));
        }

        @ParameterizedTest
        @EnumSource(value = DocDocumentStatus.class, mode = EnumSource.Mode.EXCLUDE, names = {"ARCHIVED"})
        @DisplayName("非 ARCHIVED 状态不是终态")
        void nonArchivedIsNotTerminal(DocDocumentStatus status) {
            assertFalse(stateMachine.isTerminal(status));
        }
    }

    @Nested
    @DisplayName("allowedTargets 测试")
    class AllowedTargetsTests {

        @Test
        @DisplayName("PENDING 的可达状态")
        void pendingAllowedTargets() {
            Set<DocDocumentStatus> targets = stateMachine.allowedTargets(DocDocumentStatus.PENDING);
            assertEquals(4, targets.size());
            assertTrue(targets.contains(DocDocumentStatus.RUNNING));
            assertTrue(targets.contains(DocDocumentStatus.GENERATED));
            assertTrue(targets.contains(DocDocumentStatus.FAILED));
            assertTrue(targets.contains(DocDocumentStatus.OBSOLETE));
        }

        @Test
        @DisplayName("RUNNING 的可达状态")
        void runningAllowedTargets() {
            Set<DocDocumentStatus> targets = stateMachine.allowedTargets(DocDocumentStatus.RUNNING);
            assertEquals(3, targets.size());
            assertTrue(targets.contains(DocDocumentStatus.GENERATED));
            assertTrue(targets.contains(DocDocumentStatus.FAILED));
            assertTrue(targets.contains(DocDocumentStatus.OBSOLETE));
        }

        @Test
        @DisplayName("GENERATED 的可达状态")
        void generatedAllowedTargets() {
            Set<DocDocumentStatus> targets = stateMachine.allowedTargets(DocDocumentStatus.GENERATED);
            assertEquals(2, targets.size());
            assertTrue(targets.contains(DocDocumentStatus.ARCHIVED));
            assertTrue(targets.contains(DocDocumentStatus.OBSOLETE));
        }

        @Test
        @DisplayName("FAILED 的可达状态")
        void failedAllowedTargets() {
            Set<DocDocumentStatus> targets = stateMachine.allowedTargets(DocDocumentStatus.FAILED);
            assertEquals(3, targets.size());
            assertTrue(targets.contains(DocDocumentStatus.PENDING));
            assertTrue(targets.contains(DocDocumentStatus.RUNNING));
            assertTrue(targets.contains(DocDocumentStatus.OBSOLETE));
        }

        @Test
        @DisplayName("ARCHIVED 的可达状态为空")
        void archivedAllowedTargetsEmpty() {
            Set<DocDocumentStatus> targets = stateMachine.allowedTargets(DocDocumentStatus.ARCHIVED);
            assertTrue(targets.isEmpty());
        }

        @Test
        @DisplayName("OBSOLETE 的可达状态")
        void obsoleteAllowedTargets() {
            Set<DocDocumentStatus> targets = stateMachine.allowedTargets(DocDocumentStatus.OBSOLETE);
            assertEquals(2, targets.size());
            assertTrue(targets.contains(DocDocumentStatus.PENDING));
            assertTrue(targets.contains(DocDocumentStatus.RUNNING));
        }
    }

    @Nested
    @DisplayName("静态便捷方法测试")
    class StaticMethodTests {

        @Test
        @DisplayName("checkTransition 静态方法正常调用")
        void staticCheckTransition() {
            assertDoesNotThrow(() ->
                DocDocumentStateMachine.checkTransition(DocDocumentStatus.PENDING, DocDocumentStatus.RUNNING));
        }

        @Test
        @DisplayName("checkTransition 静态方法抛出异常")
        void staticCheckTransitionThrows() {
            assertThrows(ServiceException.class,
                () -> DocDocumentStateMachine.checkTransition(DocDocumentStatus.ARCHIVED, DocDocumentStatus.PENDING));
        }
    }

    @Nested
    @DisplayName("单例实例测试")
    class SingletonTests {

        @Test
        @DisplayName("INSTANCE 是同一个实例")
        void instanceIsSame() {
            assertSame(DocDocumentStateMachine.INSTANCE, DocDocumentStateMachine.INSTANCE);
        }
    }
}