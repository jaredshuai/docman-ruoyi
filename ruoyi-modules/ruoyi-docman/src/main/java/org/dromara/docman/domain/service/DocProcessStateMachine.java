package org.dromara.docman.domain.service;

import org.dromara.common.core.statemachine.StateMachine;
import org.dromara.docman.domain.enums.DocProcessConfigStatus;

import java.util.EnumSet;
import java.util.Map;

public final class DocProcessStateMachine extends StateMachine<DocProcessConfigStatus> {

    public static final DocProcessStateMachine INSTANCE = new DocProcessStateMachine();

    private DocProcessStateMachine() {
        super(Map.of(
            DocProcessConfigStatus.PENDING, EnumSet.of(DocProcessConfigStatus.RUNNING),
            DocProcessConfigStatus.RUNNING, EnumSet.of(DocProcessConfigStatus.COMPLETED),
            DocProcessConfigStatus.COMPLETED, EnumSet.noneOf(DocProcessConfigStatus.class)
        ));
    }

    public static void checkTransition(DocProcessConfigStatus current, DocProcessConfigStatus target) {
        INSTANCE.ensureTransition(current, target);
    }
}
