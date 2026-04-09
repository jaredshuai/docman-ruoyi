package org.dromara.docman.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.docman.domain.bo.DocWorkflowNodeTaskBo;
import org.dromara.docman.domain.bo.DocWorkflowTemplateBo;
import org.dromara.docman.domain.bo.DocWorkflowTemplateNodeBo;
import org.dromara.docman.domain.entity.DocWorkflowNodeTask;
import org.dromara.docman.domain.entity.DocWorkflowTemplate;
import org.dromara.docman.domain.entity.DocWorkflowTemplateNode;
import org.dromara.docman.domain.vo.DocWorkflowNodeTaskVo;
import org.dromara.docman.domain.vo.DocWorkflowTemplateNodeVo;
import org.dromara.docman.domain.vo.DocWorkflowTemplateVo;
import org.dromara.docman.mapper.DocWorkflowNodeTaskMapper;
import org.dromara.docman.mapper.DocWorkflowTemplateMapper;
import org.dromara.docman.mapper.DocWorkflowTemplateNodeMapper;
import org.dromara.docman.service.IDocWorkflowTemplateService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DocWorkflowTemplateServiceImpl implements IDocWorkflowTemplateService {

    private final DocWorkflowTemplateMapper templateMapper;
    private final DocWorkflowTemplateNodeMapper nodeMapper;
    private final DocWorkflowNodeTaskMapper taskMapper;

    @Override
    public List<DocWorkflowTemplateVo> listByProjectType(String projectTypeCode) {
        LambdaQueryWrapper<DocWorkflowTemplate> wrapper = new LambdaQueryWrapper<DocWorkflowTemplate>()
            .orderByAsc(DocWorkflowTemplate::getSortOrder)
            .orderByAsc(DocWorkflowTemplate::getCreateTime);
        if (projectTypeCode != null && !projectTypeCode.isBlank()) {
            wrapper.eq(DocWorkflowTemplate::getProjectTypeCode, projectTypeCode);
        }
        List<DocWorkflowTemplateVo> templates = templateMapper.selectVoList(wrapper);
        templates.forEach(this::fillNodes);
        return templates;
    }

    @Override
    public DocWorkflowTemplateVo queryById(Long id) {
        DocWorkflowTemplateVo vo = templateMapper.selectVoById(id);
        if (vo == null) {
            throw new ServiceException("工作流模板不存在");
        }
        fillNodes(vo);
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long save(DocWorkflowTemplateBo bo) {
        ensureTemplateCodeUnique(bo);
        DocWorkflowTemplate template = new DocWorkflowTemplate();
        template.setId(bo.getId());
        template.setCode(bo.getCode());
        template.setName(bo.getName());
        template.setProjectTypeCode(bo.getProjectTypeCode());
        template.setDescription(bo.getDescription());
        template.setDefaultFlag(Boolean.TRUE.equals(bo.getDefaultFlag()));
        template.setSortOrder(bo.getSortOrder());
        template.setStatus(defaultStatus(bo.getStatus()));
        if (bo.getId() == null) {
            templateMapper.insert(template);
        } else {
            templateMapper.updateById(template);
            deleteNodesByTemplateId(template.getId());
        }
        saveNodes(template.getId(), bo.getNodes());
        return template.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteByIds(List<Long> ids) {
        for (Long id : ids) {
            if (templateMapper.selectById(id) != null) {
                deleteNodesByTemplateId(id);
                templateMapper.deleteById(id);
            }
        }
    }

    private void fillNodes(DocWorkflowTemplateVo template) {
        List<DocWorkflowTemplateNodeVo> nodes = nodeMapper.selectVoList(new LambdaQueryWrapper<DocWorkflowTemplateNode>()
            .eq(DocWorkflowTemplateNode::getTemplateId, template.getId())
            .orderByAsc(DocWorkflowTemplateNode::getSortOrder)
            .orderByAsc(DocWorkflowTemplateNode::getCreateTime));
        for (DocWorkflowTemplateNodeVo node : nodes) {
            List<DocWorkflowNodeTaskVo> tasks = taskMapper.selectVoList(new LambdaQueryWrapper<DocWorkflowNodeTask>()
                .eq(DocWorkflowNodeTask::getNodeId, node.getId())
                .orderByAsc(DocWorkflowNodeTask::getSortOrder)
                .orderByAsc(DocWorkflowNodeTask::getCreateTime));
            node.setTasks(tasks);
        }
        template.setNodes(nodes);
    }

    private void saveNodes(Long templateId, List<DocWorkflowTemplateNodeBo> nodeBos) {
        if (nodeBos == null) {
            return;
        }
        for (DocWorkflowTemplateNodeBo nodeBo : nodeBos) {
            DocWorkflowTemplateNode node = new DocWorkflowTemplateNode();
            node.setTemplateId(templateId);
            node.setNodeCode(nodeBo.getNodeCode());
            node.setNodeName(nodeBo.getNodeName());
            node.setSortOrder(nodeBo.getSortOrder());
            node.setDescription(nodeBo.getDescription());
            node.setStatus(defaultStatus(nodeBo.getStatus()));
            nodeMapper.insert(node);
            saveTasks(node.getId(), nodeBo.getTasks());
        }
    }

    private void saveTasks(Long nodeId, List<DocWorkflowNodeTaskBo> taskBos) {
        if (taskBos == null) {
            return;
        }
        for (DocWorkflowNodeTaskBo taskBo : taskBos) {
            DocWorkflowNodeTask task = new DocWorkflowNodeTask();
            task.setNodeId(nodeId);
            task.setTaskCode(taskBo.getTaskCode());
            task.setTaskName(taskBo.getTaskName());
            task.setTaskType(taskBo.getTaskType());
            task.setRequiredFlag(Boolean.TRUE.equals(taskBo.getRequiredFlag()));
            task.setSortOrder(taskBo.getSortOrder());
            task.setCompletionRule(taskBo.getCompletionRule());
            task.setPluginCodes(taskBo.getPluginCodes());
            task.setDescription(taskBo.getDescription());
            task.setStatus(defaultStatus(taskBo.getStatus()));
            taskMapper.insert(task);
        }
    }

    private void deleteNodesByTemplateId(Long templateId) {
        List<DocWorkflowTemplateNode> nodes = nodeMapper.selectList(new LambdaQueryWrapper<DocWorkflowTemplateNode>()
            .eq(DocWorkflowTemplateNode::getTemplateId, templateId));
        for (DocWorkflowTemplateNode node : nodes) {
            taskMapper.delete(new LambdaQueryWrapper<DocWorkflowNodeTask>()
                .eq(DocWorkflowNodeTask::getNodeId, node.getId()));
            nodeMapper.deleteById(node.getId());
        }
    }

    private void ensureTemplateCodeUnique(DocWorkflowTemplateBo bo) {
        DocWorkflowTemplate existing = templateMapper.selectOne(new LambdaQueryWrapper<DocWorkflowTemplate>()
            .eq(DocWorkflowTemplate::getCode, bo.getCode()));
        if (existing != null && !existing.getId().equals(bo.getId())) {
            throw new ServiceException("工作流模板编码已存在");
        }
    }

    private String defaultStatus(String status) {
        return (status == null || status.isBlank()) ? "active" : status;
    }
}
