package org.dromara.docman.service;

import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.docman.domain.bo.DocDocumentRecordBo;
import org.dromara.docman.domain.vo.DocDocumentRecordVo;
import org.dromara.docman.plugin.PluginResult;

public interface IDocDocumentRecordService {

    TableDataInfo<DocDocumentRecordVo> queryPageList(Long projectId, PageQuery pageQuery);

    DocDocumentRecordVo queryById(Long id);

    void recordUpload(DocDocumentRecordBo bo);

    void recordPluginGenerated(Long projectId, String pluginId, PluginResult.GeneratedFile file);

    void markObsoleteByProjectId(Long projectId);
}
