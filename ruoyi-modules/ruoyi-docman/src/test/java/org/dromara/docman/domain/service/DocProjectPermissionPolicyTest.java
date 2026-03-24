package org.dromara.docman.domain.service;

import org.dromara.docman.domain.enums.DocProjectAction;
import org.dromara.docman.domain.enums.DocProjectRole;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@Tag("dev")
@Tag("prod")
@Tag("local")
class DocProjectPermissionPolicyTest {

    private final DocProjectPermissionPolicy policy = new DocProjectPermissionPolicy();

    // ==================== OWNER权限测试 ====================

    @Test
    void ownerCanViewProject() {
        assertTrue(policy.can(DocProjectRole.OWNER, DocProjectAction.VIEW_PROJECT));
    }

    @Test
    void ownerCanEditProject() {
        assertTrue(policy.can(DocProjectRole.OWNER, DocProjectAction.EDIT_PROJECT));
    }

    @Test
    void ownerCanDeleteProject() {
        assertTrue(policy.can(DocProjectRole.OWNER, DocProjectAction.DELETE_PROJECT));
    }

    @Test
    void ownerCanViewDocument() {
        assertTrue(policy.can(DocProjectRole.OWNER, DocProjectAction.VIEW_DOCUMENT));
    }

    @Test
    void ownerCanUploadDocument() {
        assertTrue(policy.can(DocProjectRole.OWNER, DocProjectAction.UPLOAD_DOCUMENT));
    }

    @Test
    void ownerCanViewProcess() {
        assertTrue(policy.can(DocProjectRole.OWNER, DocProjectAction.VIEW_PROCESS));
    }

    @Test
    void ownerCanBindProcess() {
        assertTrue(policy.can(DocProjectRole.OWNER, DocProjectAction.BIND_PROCESS));
    }

    @Test
    void ownerCanStartProcess() {
        assertTrue(policy.can(DocProjectRole.OWNER, DocProjectAction.START_PROCESS));
    }

    @Test
    void ownerCanViewArchive() {
        assertTrue(policy.can(DocProjectRole.OWNER, DocProjectAction.VIEW_ARCHIVE));
    }

    @Test
    void ownerCanArchiveProject() {
        assertTrue(policy.can(DocProjectRole.OWNER, DocProjectAction.ARCHIVE_PROJECT));
    }

    @Test
    void ownerHasAllActions() {
        Set<DocProjectAction> actions = policy.allowedActions(DocProjectRole.OWNER);
        assertEquals(10, actions.size());
        assertTrue(actions.containsAll(Set.of(DocProjectAction.values())));
    }

    // ==================== EDITOR权限测试 ====================

    @Test
    void editorCanViewProject() {
        assertTrue(policy.can(DocProjectRole.EDITOR, DocProjectAction.VIEW_PROJECT));
    }

    @Test
    void editorCanEditProject() {
        assertTrue(policy.can(DocProjectRole.EDITOR, DocProjectAction.EDIT_PROJECT));
    }

    @Test
    void editorCannotDeleteProject() {
        assertFalse(policy.can(DocProjectRole.EDITOR, DocProjectAction.DELETE_PROJECT));
    }

    @Test
    void editorCanViewDocument() {
        assertTrue(policy.can(DocProjectRole.EDITOR, DocProjectAction.VIEW_DOCUMENT));
    }

    @Test
    void editorCanUploadDocument() {
        assertTrue(policy.can(DocProjectRole.EDITOR, DocProjectAction.UPLOAD_DOCUMENT));
    }

    @Test
    void editorCanViewProcess() {
        assertTrue(policy.can(DocProjectRole.EDITOR, DocProjectAction.VIEW_PROCESS));
    }

    @Test
    void editorCannotBindProcess() {
        assertFalse(policy.can(DocProjectRole.EDITOR, DocProjectAction.BIND_PROCESS));
    }

    @Test
    void editorCannotStartProcess() {
        assertFalse(policy.can(DocProjectRole.EDITOR, DocProjectAction.START_PROCESS));
    }

    @Test
    void editorCanViewArchive() {
        assertTrue(policy.can(DocProjectRole.EDITOR, DocProjectAction.VIEW_ARCHIVE));
    }

    @Test
    void editorCannotArchiveProject() {
        assertFalse(policy.can(DocProjectRole.EDITOR, DocProjectAction.ARCHIVE_PROJECT));
    }

    @Test
    void editorHasSixActions() {
        Set<DocProjectAction> actions = policy.allowedActions(DocProjectRole.EDITOR);
        assertEquals(6, actions.size());
        assertTrue(actions.contains(DocProjectAction.VIEW_PROJECT));
        assertTrue(actions.contains(DocProjectAction.EDIT_PROJECT));
        assertTrue(actions.contains(DocProjectAction.VIEW_DOCUMENT));
        assertTrue(actions.contains(DocProjectAction.UPLOAD_DOCUMENT));
        assertTrue(actions.contains(DocProjectAction.VIEW_PROCESS));
        assertTrue(actions.contains(DocProjectAction.VIEW_ARCHIVE));
    }

    // ==================== VIEWER权限测试 ====================

    @Test
    void viewerCanViewProject() {
        assertTrue(policy.can(DocProjectRole.VIEWER, DocProjectAction.VIEW_PROJECT));
    }

    @Test
    void viewerCannotEditProject() {
        assertFalse(policy.can(DocProjectRole.VIEWER, DocProjectAction.EDIT_PROJECT));
    }

    @Test
    void viewerCannotDeleteProject() {
        assertFalse(policy.can(DocProjectRole.VIEWER, DocProjectAction.DELETE_PROJECT));
    }

    @Test
    void viewerCanViewDocument() {
        assertTrue(policy.can(DocProjectRole.VIEWER, DocProjectAction.VIEW_DOCUMENT));
    }

    @Test
    void viewerCannotUploadDocument() {
        assertFalse(policy.can(DocProjectRole.VIEWER, DocProjectAction.UPLOAD_DOCUMENT));
    }

    @Test
    void viewerCanViewProcess() {
        assertTrue(policy.can(DocProjectRole.VIEWER, DocProjectAction.VIEW_PROCESS));
    }

    @Test
    void viewerCannotBindProcess() {
        assertFalse(policy.can(DocProjectRole.VIEWER, DocProjectAction.BIND_PROCESS));
    }

    @Test
    void viewerCannotStartProcess() {
        assertFalse(policy.can(DocProjectRole.VIEWER, DocProjectAction.START_PROCESS));
    }

    @Test
    void viewerCanViewArchive() {
        assertTrue(policy.can(DocProjectRole.VIEWER, DocProjectAction.VIEW_ARCHIVE));
    }

    @Test
    void viewerCannotArchiveProject() {
        assertFalse(policy.can(DocProjectRole.VIEWER, DocProjectAction.ARCHIVE_PROJECT));
    }

    @Test
    void viewerHasFourActions() {
        Set<DocProjectAction> actions = policy.allowedActions(DocProjectRole.VIEWER);
        assertEquals(4, actions.size());
        assertTrue(actions.contains(DocProjectAction.VIEW_PROJECT));
        assertTrue(actions.contains(DocProjectAction.VIEW_DOCUMENT));
        assertTrue(actions.contains(DocProjectAction.VIEW_PROCESS));
        assertTrue(actions.contains(DocProjectAction.VIEW_ARCHIVE));
    }

    // ==================== allowedActions不可变测试 ====================

    @Test
    void allowedActionsReturnsImmutableSet() {
        Set<DocProjectAction> actions = policy.allowedActions(DocProjectRole.OWNER);
        assertThrows(UnsupportedOperationException.class, () ->
            actions.add(DocProjectAction.VIEW_PROJECT));
    }
}