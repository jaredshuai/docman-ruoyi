package org.dromara.docman.domain.service;

import org.dromara.common.core.exception.ServiceException;
import org.dromara.docman.domain.enums.DocProcessConfigStatus;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("dev")
@Tag("prod")
@Tag("local")
class DocProcessStateMachineTest {

    private final DocProcessStateMachine stateMachine = DocProcessStateMachine.INSTANCE;

    @Test
    void shouldAllowPendingToRunningTransition() {
        assertDoesNotThrow(() ->
            stateMachine.ensureTransition(DocProcessConfigStatus.PENDING, DocProcessConfigStatus.RUNNING));
        assertTrue(stateMachine.canTransition(DocProcessConfigStatus.PENDING, DocProcessConfigStatus.RUNNING));
    }

    @Test
    void shouldAllowRunningToCompletedTransition() {
        assertDoesNotThrow(() ->
            stateMachine.ensureTransition(DocProcessConfigStatus.RUNNING, DocProcessConfigStatus.COMPLETED));
        assertTrue(stateMachine.canTransition(DocProcessConfigStatus.RUNNING, DocProcessConfigStatus.COMPLETED));
    }

    @Test
    void shouldRejectPendingToCompletedTransition() {
        ServiceException ex = assertThrows(ServiceException.class, () ->
            stateMachine.ensureTransition(DocProcessConfigStatus.PENDING, DocProcessConfigStatus.COMPLETED));
        assertTrue(ex.getMessage().contains("非法状态变更"));
        assertFalse(stateMachine.canTransition(DocProcessConfigStatus.PENDING, DocProcessConfigStatus.COMPLETED));
    }

    @Test
    void shouldRejectCompletedToRunningTransition() {
        ServiceException ex = assertThrows(ServiceException.class, () ->
            stateMachine.ensureTransition(DocProcessConfigStatus.COMPLETED, DocProcessConfigStatus.RUNNING));
        assertTrue(ex.getMessage().contains("非法状态变更"));
        assertFalse(stateMachine.canTransition(DocProcessConfigStatus.COMPLETED, DocProcessConfigStatus.RUNNING));
    }

    @Test
    void shouldAllowSameStateTransition() {
        assertDoesNotThrow(() ->
            stateMachine.ensureTransition(DocProcessConfigStatus.RUNNING, DocProcessConfigStatus.RUNNING));
        assertTrue(stateMachine.canTransition(DocProcessConfigStatus.RUNNING, DocProcessConfigStatus.RUNNING));
    }

    @Test
    void shouldExposeAllowedTargets() {
        Set<DocProcessConfigStatus> pendingTargets = stateMachine.allowedTargets(DocProcessConfigStatus.PENDING);
        Set<DocProcessConfigStatus> runningTargets = stateMachine.allowedTargets(DocProcessConfigStatus.RUNNING);
        Set<DocProcessConfigStatus> completedTargets = stateMachine.allowedTargets(DocProcessConfigStatus.COMPLETED);

        assertEquals(Set.of(DocProcessConfigStatus.RUNNING), pendingTargets);
        assertEquals(Set.of(DocProcessConfigStatus.COMPLETED), runningTargets);
        assertTrue(completedTargets.isEmpty());
    }

    @Test
    void shouldIdentifyTerminalState() {
        assertFalse(stateMachine.isTerminal(DocProcessConfigStatus.PENDING));
        assertFalse(stateMachine.isTerminal(DocProcessConfigStatus.RUNNING));
        assertTrue(stateMachine.isTerminal(DocProcessConfigStatus.COMPLETED));
    }

    @Test
    void shouldDelegateStaticCheckTransition() {
        assertDoesNotThrow(() ->
            DocProcessStateMachine.checkTransition(DocProcessConfigStatus.PENDING, DocProcessConfigStatus.RUNNING));
        assertThrows(ServiceException.class, () ->
            DocProcessStateMachine.checkTransition(DocProcessConfigStatus.COMPLETED, DocProcessConfigStatus.PENDING));
    }
}
