package org.dromara.docman.service;

import org.dromara.docman.domain.bo.DocProjectTypeBo;
import org.dromara.docman.domain.vo.DocProjectTypeVo;

import java.util.List;

public interface IDocProjectTypeService {

    List<DocProjectTypeVo> listAll();

    DocProjectTypeVo queryById(Long id);

    Long save(DocProjectTypeBo bo);

    void deleteByIds(List<Long> ids);
}
