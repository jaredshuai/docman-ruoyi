package org.dromara.docman.domain.service;

import org.dromara.common.core.exception.ServiceException;
import org.dromara.docman.domain.enums.DocProjectStatus;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@Tag("dev")
@Tag("prod")
@Tag("local")
class DocProjectStateMachineTest {

    private final DocProjectStateMachine stateMachine = DocProjectStateMachine.INSTANCE;

    @Test
    void shouldAllowLegalTransitionFromActiveToArchived() {
        assertDoesNotThrow(() -> stateMachine.ensureTransition(DocProjectStatus.ACTIVE, DocProjectStatus.ARCHIVED));
    }

    @Test
    void shouldThrowOnIllegalTransitionFromArchivedToActive() {
        ServiceException ex = assertThrows(ServiceException.class,
            () -> stateMachine.ensureTransition(DocProjectStatus.ARCHIVED, DocProjectStatus.ACTIVE));
        assertTrue(ex.getMessage().contains("非法状态变更"));
    }

    @Test
    void shouldAllowSameStateTransitionForActive() {
        assertDoesNotThrow(() -> stateMachine.ensureTransition(DocProjectStatus.ACTIVE, DocProjectStatus.ACTIVE));
    }

    @Test
    void shouldAllowSameStateTransitionForArchived() {
        assertDoesNotThrow(() -> stateMachine.ensureTransition(DocProjectStatus.ARCHIVED, DocProjectStatus.ARCHIVED));
    }

    @Test
    void shouldReturnArchivedAsAllowedTargetFromActive() {
        var targets = stateMachine.allowedTargets(DocProjectStatus.ACTIVE);
        assertEquals(1, targets.size());
        assertTrue(targets.contains(DocProjectStatus.ARCHIVED));
    }

    @Test
    void shouldReturnEmptyAllowedTargetsFromArchived() {
        var targets = stateMachine.allowedTargets(DocProjectStatus.ARCHIVED);
        assertTrue(targets.isEmpty());
    }

    @Test
    void activeShouldNotBeTerminal() {
        assertFalse(stateMachine.isTerminal(DocProjectStatus.ACTIVE));
    }

    @Test
    void archivedShouldBeTerminal() {
        assertTrue(stateMachine.isTerminal(DocProjectStatus.ARCHIVED));
    }

    @Test
    void checkTransitionShouldDelegateToInstance() {
        assertDoesNotThrow(() -> DocProjectStateMachine.checkTransition(DocProjectStatus.ACTIVE, DocProjectStatus.ARCHIVED));
        assertThrows(ServiceException.class,
            () -> DocProjectStateMachine.checkTransition(DocProjectStatus.ARCHIVED, DocProjectStatus.ACTIVE));
    }
}