package org.dromara.docman.service;

import org.dromara.docman.domain.bo.DocProjectDrawingBo;
import org.dromara.docman.domain.vo.DocProjectDrawingVo;

import java.util.List;

public interface IDocProjectDrawingService {

    List<DocProjectDrawingVo> listByProject(Long projectId);

    DocProjectDrawingVo queryById(Long id);

    Long save(DocProjectDrawingBo bo);

    void deleteByIds(List<Long> ids);
}
