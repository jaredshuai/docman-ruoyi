package org.dromara.docman.service;

import org.dromara.docman.domain.bo.DocProjectVisaBo;
import org.dromara.docman.domain.vo.DocProjectVisaVo;

import java.util.List;

public interface IDocProjectVisaService {

    List<DocProjectVisaVo> listByProject(Long projectId);

    DocProjectVisaVo queryById(Long id);

    Long save(DocProjectVisaBo bo);

    void deleteByIds(List<Long> ids);
}
