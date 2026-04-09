package org.dromara.docman.domain.service;

import org.dromara.common.core.statemachine.StateMachine;
import org.dromara.docman.domain.enums.DocProjectRuntimeStatus;

import java.util.EnumSet;
import java.util.Map;

/**
 * 项目运行时状态机。
 */
public final class DocProjectRuntimeStateMachine extends StateMachine<DocProjectRuntimeStatus> {

    public static final DocProjectRuntimeStateMachine INSTANCE = new DocProjectRuntimeStateMachine();

    private DocProjectRuntimeStateMachine() {
        super(Map.of(
            DocProjectRuntimeStatus.RUNNING, EnumSet.of(DocProjectRuntimeStatus.COMPLETED),
            DocProjectRuntimeStatus.COMPLETED, EnumSet.noneOf(DocProjectRuntimeStatus.class)
        ));
    }

    public static void checkTransition(DocProjectRuntimeStatus current, DocProjectRuntimeStatus target) {
        INSTANCE.ensureTransition(current, target);
    }
}
