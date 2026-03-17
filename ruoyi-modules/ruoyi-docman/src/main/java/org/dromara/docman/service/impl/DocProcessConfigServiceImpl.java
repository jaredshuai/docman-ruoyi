package org.dromara.docman.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import org.dromara.docman.domain.entity.DocProcessConfig;
import org.dromara.docman.domain.enums.DocProcessConfigStatus;
import org.dromara.docman.mapper.DocProcessConfigMapper;
import org.dromara.docman.service.IDocProcessConfigService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DocProcessConfigServiceImpl implements IDocProcessConfigService {

    private final DocProcessConfigMapper processConfigMapper;

    @Override
    public DocProcessConfig queryByInstanceId(Long instanceId) {
        return processConfigMapper.selectOne(
            new LambdaQueryWrapper<DocProcessConfig>()
                .eq(DocProcessConfig::getInstanceId, instanceId)
        );
    }

    @Override
    public List<DocProcessConfig> listByStatus(DocProcessConfigStatus status) {
        return processConfigMapper.selectList(
            new LambdaQueryWrapper<DocProcessConfig>()
                .eq(DocProcessConfig::getStatus, status.getCode())
                .select(DocProcessConfig::getProjectId)
        );
    }

    @Override
    public void updateStatus(Long id, String status) {
        processConfigMapper.update(
            null,
            new LambdaUpdateWrapper<DocProcessConfig>()
                .set(DocProcessConfig::getStatus, status)
                .eq(DocProcessConfig::getId, id)
        );
    }
}
