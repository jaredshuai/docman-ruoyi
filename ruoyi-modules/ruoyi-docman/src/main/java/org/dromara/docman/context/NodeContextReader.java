package org.dromara.docman.context;

import org.dromara.docman.domain.entity.DocNodeContext;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 节点上下文只读访问器
 */
public class NodeContextReader {

    private final List<DocNodeContext> contexts;

    public NodeContextReader(List<DocNodeContext> contexts) {
        this.contexts = Collections.unmodifiableList(contexts);
    }

    public Object getStructuredField(String nodeCode, String fieldName) {
        return contexts.stream()
            .filter(c -> c.getNodeCode().equals(nodeCode))
            .findFirst()
            .map(c -> c.getNodeVariables().get(fieldName))
            .orElse(null);
    }

    public Object getProcessVariable(String fieldName) {
        return contexts.stream()
            .map(DocNodeContext::getProcessVariables)
            .filter(map -> map != null && map.containsKey(fieldName))
            .map(map -> map.get(fieldName))
            .findFirst()
            .orElse(null);
    }

    public Object getDocumentFact(String fieldName) {
        return contexts.stream()
            .map(DocNodeContext::getDocumentFacts)
            .filter(map -> map != null && map.containsKey(fieldName))
            .map(map -> map.get(fieldName))
            .reduce((first, second) -> second)
            .orElse(null);
    }

    public String getUnstructuredContent(String nodeCode, String key) {
        return contexts.stream()
            .filter(c -> c.getNodeCode().equals(nodeCode))
            .findFirst()
            .map(c -> c.getUnstructuredContent().get(key))
            .orElse(null);
    }

    public Map<String, Object> getAllReadableFields() {
        Map<String, Object> merged = new LinkedHashMap<>();
        for (DocNodeContext ctx : contexts) {
            if (ctx.getProcessVariables() != null) {
                merged.putAll(ctx.getProcessVariables());
            }
            if (ctx.getDocumentFacts() != null) {
                merged.putAll(ctx.getDocumentFacts());
            }
            if (ctx.getNodeVariables() != null) {
                merged.putAll(ctx.getNodeVariables());
            }
        }
        return Collections.unmodifiableMap(merged);
    }

    public Map<String, Object> getAllDocumentFacts() {
        Map<String, Object> merged = new LinkedHashMap<>();
        for (DocNodeContext ctx : contexts) {
            if (ctx.getDocumentFacts() != null) {
                merged.putAll(ctx.getDocumentFacts());
            }
        }
        return Collections.unmodifiableMap(merged);
    }

    public Map<String, String> getAllUnstructuredContent() {
        Map<String, String> merged = new LinkedHashMap<>();
        for (DocNodeContext ctx : contexts) {
            if (ctx.getUnstructuredContent() != null) {
                merged.putAll(ctx.getUnstructuredContent());
            }
        }
        return Collections.unmodifiableMap(merged);
    }
}
