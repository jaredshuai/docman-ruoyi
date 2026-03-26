package org.dromara.docman.knowledge;

import java.util.List;

/**
 * 知识库检索客户端接口。
 * <p>
 * 设计意图：为插件提供知识库检索能力，支持按查询词获取相关文档片段。
 * 计划对接点：AI 生成插件、数据提取插件的知识增强模式。
 * 当前状态：骨架接口，尚未接入业务流程。
 */
public interface KnowledgeClient {

    List<KnowledgeResult> search(String query, int topK);

    record KnowledgeResult(String content, String source) {}
}
