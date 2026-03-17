package org.dromara.docman.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.docman.domain.entity.DocPluginExecutionLog;
import org.dromara.docman.domain.enums.DocProjectAction;
import org.dromara.docman.domain.vo.DocPluginExecutionLogVo;
import org.dromara.docman.mapper.DocPluginExecutionLogMapper;
import org.dromara.docman.service.IDocPluginExecutionLogService;
import org.dromara.docman.service.IDocProjectAccessService;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DocPluginExecutionLogServiceImpl implements IDocPluginExecutionLogService {

    private final DocPluginExecutionLogMapper pluginExecutionLogMapper;
    private final IDocProjectAccessService projectAccessService;

    @Override
    public TableDataInfo<DocPluginExecutionLogVo> queryPageList(Long projectId, Long processInstanceId,
                                                                String nodeCode, String pluginId,
                                                                PageQuery pageQuery) {
        projectAccessService.assertAction(projectId, DocProjectAction.VIEW_PROJECT);
        LambdaQueryWrapper<DocPluginExecutionLog> lqw = new LambdaQueryWrapper<DocPluginExecutionLog>()
            .eq(DocPluginExecutionLog::getProjectId, projectId)
            .eq(processInstanceId != null, DocPluginExecutionLog::getProcessInstanceId, processInstanceId)
            .eq(StringUtils.isNotBlank(nodeCode), DocPluginExecutionLog::getNodeCode, nodeCode)
            .eq(StringUtils.isNotBlank(pluginId), DocPluginExecutionLog::getPluginId, pluginId)
            .orderByDesc(DocPluginExecutionLog::getCreateTime);
        Page<DocPluginExecutionLogVo> page = pluginExecutionLogMapper.selectVoPage(pageQuery.build(), lqw);
        return TableDataInfo.build(page);
    }
}
