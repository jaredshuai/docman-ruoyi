package org.dromara.docman.service;

import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.docman.domain.bo.DocProjectBo;
import org.dromara.docman.domain.entity.DocProject;
import org.dromara.docman.domain.enums.DocProjectStatus;
import org.dromara.docman.domain.vo.DocProjectVo;

import java.util.Collection;
import java.util.List;

public interface IDocProjectService {

    TableDataInfo<DocProjectVo> queryPageList(DocProjectBo bo, PageQuery pageQuery);

    List<DocProjectVo> queryMyList(DocProjectBo bo);

    DocProjectVo queryById(Long id);

    Long insertProject(DocProjectBo bo);

    Boolean updateProject(DocProjectBo bo);

    Boolean deleteByIds(List<Long> ids);

    int retryPendingNasDirectories();

    void assertViewPermission(Long projectId);

    List<DocProject> listByIdsAndStatus(Collection<Long> ids, DocProjectStatus status);
}
