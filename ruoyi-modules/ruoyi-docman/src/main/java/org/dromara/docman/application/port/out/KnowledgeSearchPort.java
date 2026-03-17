package org.dromara.docman.application.port.out;

import org.dromara.common.core.port.OutboundPort;

import java.util.List;

@OutboundPort("RAG 知识检索服务")
public interface KnowledgeSearchPort {

    List<KnowledgeResult> search(String query, int topK);

    record KnowledgeResult(String content, String source) {
    }
}
