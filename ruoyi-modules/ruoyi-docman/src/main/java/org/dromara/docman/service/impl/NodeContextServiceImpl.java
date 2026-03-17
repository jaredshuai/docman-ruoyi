package org.dromara.docman.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.dromara.docman.context.NodeContextReader;
import org.dromara.docman.domain.entity.DocNodeContext;
import org.dromara.docman.mapper.DocNodeContextMapper;
import org.dromara.docman.service.INodeContextService;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class NodeContextServiceImpl implements INodeContextService {

    private final DocNodeContextMapper contextMapper;

    @Override
    public DocNodeContext getOrCreate(Long processInstanceId, String nodeCode, Long projectId) {
        DocNodeContext existing = contextMapper.selectOne(
            new LambdaQueryWrapper<DocNodeContext>()
                .eq(DocNodeContext::getProcessInstanceId, processInstanceId)
                .eq(DocNodeContext::getNodeCode, nodeCode)
        );
        if (existing != null) {
            return existing;
        }
        DocNodeContext ctx = DocNodeContext.create(processInstanceId, nodeCode, projectId);
        contextMapper.insert(ctx);
        return ctx;
    }

    @Override
    public void putProcessVariable(Long contextId, String fieldName, Object value) {
        DocNodeContext ctx = contextMapper.selectById(contextId);
        Map<String, Object> fields = ctx.getProcessVariables();
        fields.put(fieldName, value);
        ctx.setProcessVariables(fields);
        ctx.setUpdateTime(new Date());
        contextMapper.updateById(ctx);
    }

    @Override
    public void putNodeVariable(Long contextId, String fieldName, Object value) {
        DocNodeContext ctx = contextMapper.selectById(contextId);
        Map<String, Object> fields = ctx.getNodeVariables();
        fields.put(fieldName, value);
        ctx.setNodeVariables(fields);
        ctx.setUpdateTime(new Date());
        contextMapper.updateById(ctx);
    }

    @Override
    public void putDocumentFact(Long contextId, String fieldName, Object value) {
        DocNodeContext ctx = contextMapper.selectById(contextId);
        Map<String, Object> fields = ctx.getDocumentFacts();
        fields.put(fieldName, value);
        ctx.setDocumentFacts(fields);
        ctx.setUpdateTime(new Date());
        contextMapper.updateById(ctx);
    }

    @Override
    public void putUnstructuredContent(Long contextId, String key, String text) {
        DocNodeContext ctx = contextMapper.selectById(contextId);
        Map<String, String> content = ctx.getUnstructuredContent();
        content.put(key, text);
        ctx.setUnstructuredContent(content);
        ctx.setUpdateTime(new Date());
        contextMapper.updateById(ctx);
    }

    @Override
    public NodeContextReader buildReader(Long processInstanceId) {
        List<DocNodeContext> contexts = contextMapper.selectList(
            new LambdaQueryWrapper<DocNodeContext>()
                .eq(DocNodeContext::getProcessInstanceId, processInstanceId)
                .orderByAsc(DocNodeContext::getCreateTime)
        );
        return new NodeContextReader(contexts);
    }
}
