package org.dromara.docman.domain.service;

import org.dromara.common.core.statemachine.StateMachine;
import org.dromara.docman.domain.enums.DocDocumentStatus;

import java.util.EnumSet;
import java.util.Map;

/**
 * 文档生命周期状态机。继承框架通用 {@link StateMachine}。
 */
public final class DocDocumentStateMachine extends StateMachine<DocDocumentStatus> {

    public static final DocDocumentStateMachine INSTANCE = new DocDocumentStateMachine();

    private DocDocumentStateMachine() {
        super(Map.of(
            DocDocumentStatus.PENDING, EnumSet.of(DocDocumentStatus.RUNNING, DocDocumentStatus.GENERATED, DocDocumentStatus.FAILED, DocDocumentStatus.OBSOLETE),
            DocDocumentStatus.RUNNING, EnumSet.of(DocDocumentStatus.GENERATED, DocDocumentStatus.FAILED, DocDocumentStatus.OBSOLETE),
            DocDocumentStatus.GENERATED, EnumSet.of(DocDocumentStatus.ARCHIVED, DocDocumentStatus.OBSOLETE),
            DocDocumentStatus.FAILED, EnumSet.of(DocDocumentStatus.PENDING, DocDocumentStatus.RUNNING, DocDocumentStatus.OBSOLETE),
            DocDocumentStatus.ARCHIVED, EnumSet.noneOf(DocDocumentStatus.class),
            DocDocumentStatus.OBSOLETE, EnumSet.of(DocDocumentStatus.PENDING, DocDocumentStatus.RUNNING)
        ));
    }

    /**
     * 静态便捷方法，委托给单例实例。
     */
    public static void checkTransition(DocDocumentStatus current, DocDocumentStatus target) {
        INSTANCE.ensureTransition(current, target);
    }

    public static boolean canArchive(DocDocumentStatus status) {
        return status == DocDocumentStatus.GENERATED;
    }
}
