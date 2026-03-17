package org.dromara.docman.domain.service;

import org.dromara.common.core.statemachine.StateMachine;
import org.dromara.docman.domain.enums.DocProjectStatus;

import java.util.EnumSet;
import java.util.Map;

public final class DocProjectStateMachine extends StateMachine<DocProjectStatus> {

    public static final DocProjectStateMachine INSTANCE = new DocProjectStateMachine();

    private DocProjectStateMachine() {
        super(Map.of(
            DocProjectStatus.ACTIVE, EnumSet.of(DocProjectStatus.ARCHIVED),
            DocProjectStatus.ARCHIVED, EnumSet.noneOf(DocProjectStatus.class)
        ));
    }

    public static void checkTransition(DocProjectStatus current, DocProjectStatus target) {
        INSTANCE.ensureTransition(current, target);
    }
}
