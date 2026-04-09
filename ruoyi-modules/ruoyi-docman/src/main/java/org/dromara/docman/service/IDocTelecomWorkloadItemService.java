package org.dromara.docman.service;

import org.dromara.docman.domain.bo.DocTelecomWorkloadItemBo;
import org.dromara.docman.domain.vo.DocTelecomWorkloadItemVo;

import java.util.List;

public interface IDocTelecomWorkloadItemService {

    List<DocTelecomWorkloadItemVo> listAll();

    DocTelecomWorkloadItemVo queryById(Long id);

    Long save(DocTelecomWorkloadItemBo bo);

    void deleteByIds(List<Long> ids);
}
