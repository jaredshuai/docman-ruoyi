package org.dromara.docman.service;

import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.docman.domain.vo.DocPluginExecutionLogVo;

public interface IDocPluginExecutionLogService {

    /**
     * 分页查询插件执行日志（不含 LONGTEXT 快照字段）
     */
    TableDataInfo<DocPluginExecutionLogVo> queryPageList(Long projectId, Long processInstanceId,
                                                         String nodeCode, String pluginId,
                                                         PageQuery pageQuery);

    /**
     * 按ID查询插件执行日志详情（包含完整字段，含快照）
     *
     * @param id 日志ID
     * @return 日志详情
     */
    DocPluginExecutionLogVo queryById(Long id);
}
