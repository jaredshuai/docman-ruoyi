package org.dromara.docman.service;

import org.dromara.docman.context.NodeContextReader;
import org.dromara.docman.domain.entity.DocNodeContext;

public interface INodeContextService {

    DocNodeContext getOrCreate(Long processInstanceId, String nodeCode, Long projectId);

    void putProcessVariable(Long contextId, String fieldName, Object value);

    void putNodeVariable(Long contextId, String fieldName, Object value);

    void putDocumentFact(Long contextId, String fieldName, Object value);

    void putUnstructuredContent(Long contextId, String key, String text);

    NodeContextReader buildReader(Long processInstanceId);
}
