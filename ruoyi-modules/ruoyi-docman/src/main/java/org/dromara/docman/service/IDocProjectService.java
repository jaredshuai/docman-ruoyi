package org.dromara.docman.service;

import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.docman.domain.bo.DocProjectBo;
import org.dromara.docman.domain.vo.DocProjectVo;

import java.util.List;

public interface IDocProjectService {

    TableDataInfo<DocProjectVo> queryPageList(DocProjectBo bo, PageQuery pageQuery);

    DocProjectVo queryById(Long id);

    Long insertProject(DocProjectBo bo);

    Boolean updateProject(DocProjectBo bo);

    Boolean deleteByIds(List<Long> ids);

    void retryPendingNasDirectories();

    void assertViewPermission(Long projectId);
}
