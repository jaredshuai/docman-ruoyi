package org.dromara.docman.domain.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum DocProjectAction {

    VIEW_PROJECT("view:project"),
    EDIT_PROJECT("edit:project"),
    DELETE_PROJECT("delete:project"),
    VIEW_DOCUMENT("view:document"),
    UPLOAD_DOCUMENT("upload:document"),
    VIEW_PROCESS("view:process"),
    BIND_PROCESS("bind:process"),
    START_PROCESS("start:process"),
    VIEW_ARCHIVE("view:archive"),
    ARCHIVE_PROJECT("archive:project");

    private final String code;
}
