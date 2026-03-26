package org.dromara.docman.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.exception.ServiceException;
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

    /**
     * 分页查询插件执行日志
     * <p>
     * 列表查询排除 request_snapshot 和 result_snapshot 字段（LONGTEXT），
     * 减少数据传输量，提升查询性能。如需完整字段请使用 queryById 详情查询。
     */
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
            // 排除 LONGTEXT 字段，减少数据传输
            .select(DocPluginExecutionLog::getId, DocPluginExecutionLog::getProjectId,
                DocPluginExecutionLog::getProcessInstanceId, DocPluginExecutionLog::getNodeCode,
                DocPluginExecutionLog::getPluginId, DocPluginExecutionLog::getPluginName,
                DocPluginExecutionLog::getStatus, DocPluginExecutionLog::getCostMs,
                DocPluginExecutionLog::getGeneratedFileCount, DocPluginExecutionLog::getErrorMessage,
                DocPluginExecutionLog::getCreateTime)
            .orderByDesc(DocPluginExecutionLog::getCreateTime);
        Page<DocPluginExecutionLogVo> page = pluginExecutionLogMapper.selectVoPage(pageQuery.build(), lqw);
        return TableDataInfo.build(page);
    }

    /**
     * 按ID查询插件执行日志详情
     * <p>
     * 返回完整字段，包括 request_snapshot 和 result_snapshot（LONGTEXT）。
     * 需验证用户对所属项目的访问权限。
     *
     * @param id 日志ID
     * @return 日志详情
     */
    @Override
    public DocPluginExecutionLogVo queryById(Long id) {
        DocPluginExecutionLog log = pluginExecutionLogMapper.selectById(id);
        if (log == null) {
            throw new ServiceException("插件执行日志不存在");
        }
        // 校验项目访问权限
        projectAccessService.assertAction(log.getProjectId(), DocProjectAction.VIEW_PROJECT);
        return pluginExecutionLogMapper.selectVoById(id);
    }
}
