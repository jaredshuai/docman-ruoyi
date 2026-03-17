package org.dromara.docman.knowledge;

import java.util.List;

/**
 * @deprecated 已迁移为 application.port.out.KnowledgeSearchPort。
 */
@Deprecated(forRemoval = true)
public interface KnowledgeClient {

    List<KnowledgeResult> search(String query, int topK);

    record KnowledgeResult(String content, String source) {}
}
