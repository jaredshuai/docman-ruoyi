package org.dromara.docman.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.docman.application.port.out.ProcessEnginePort;
import org.dromara.docman.domain.entity.DocProcessConfig;
import org.dromara.docman.domain.enums.DocProcessConfigStatus;
import org.dromara.docman.domain.enums.DocProjectAction;
import org.dromara.docman.domain.service.DocProcessStateMachine;
import org.dromara.docman.mapper.DocProcessConfigMapper;
import org.dromara.docman.service.IDocProjectAccessService;
import org.dromara.docman.service.IDocProcessService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocProcessServiceImpl implements IDocProcessService {

    private final DocProcessConfigMapper configMapper;
    private final ProcessEnginePort processEnginePort;
    private final IDocProjectAccessService projectAccessService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void bindProcess(Long projectId, Long definitionId) {
        projectAccessService.assertAction(projectId, DocProjectAction.BIND_PROCESS);
        DocProcessConfig existing = getByProjectId(projectId);
        if (existing != null) {
            throw new ServiceException("该项目已绑定流程，请先解绑");
        }
        DocProcessConfig config = new DocProcessConfig();
        config.setProjectId(projectId);
        config.setDefinitionId(definitionId);
        config.setStatus(DocProcessConfigStatus.PENDING.getCode());
        configMapper.insert(config);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long startProcess(Long projectId) {
        projectAccessService.assertAction(projectId, DocProjectAction.START_PROCESS);
        DocProcessConfig config = getByProjectId(projectId);
        if (config == null) {
            throw new ServiceException("该项目未绑定流程");
        }
        DocProcessConfigStatus currentStatus = DocProcessConfigStatus.of(config.getStatus());
        DocProcessStateMachine.checkTransition(currentStatus, DocProcessConfigStatus.RUNNING);
        Long instanceId = processEnginePort.startProcess(config.getDefinitionId(), String.valueOf(projectId), LoginHelper.getUserId());
        config.setInstanceId(instanceId);
        config.setStatus(DocProcessConfigStatus.RUNNING.getCode());
        configMapper.updateById(config);
        log.info("项目流程已启动: projectId={}, instanceId={}", projectId, instanceId);
        return instanceId;
    }

    @Override
    public DocProcessConfig getByProjectId(Long projectId) {
        projectAccessService.assertAction(projectId, DocProjectAction.VIEW_PROCESS);
        return configMapper.selectOne(
            new LambdaQueryWrapper<DocProcessConfig>()
                .eq(DocProcessConfig::getProjectId, projectId)
        );
    }
}
