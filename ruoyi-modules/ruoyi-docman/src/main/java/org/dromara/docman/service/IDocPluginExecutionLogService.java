package org.dromara.docman.service;

import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.docman.domain.vo.DocPluginExecutionLogVo;

public interface IDocPluginExecutionLogService {

    TableDataInfo<DocPluginExecutionLogVo> queryPageList(Long projectId, Long processInstanceId,
                                                         String nodeCode, String pluginId,
                                                         PageQuery pageQuery);
}
