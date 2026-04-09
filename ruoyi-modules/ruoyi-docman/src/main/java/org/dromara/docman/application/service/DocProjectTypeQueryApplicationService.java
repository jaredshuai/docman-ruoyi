package org.dromara.docman.application.service;

import lombok.RequiredArgsConstructor;
import org.dromara.common.core.application.QueryApplicationService;
import org.dromara.docman.domain.vo.DocProjectTypeVo;
import org.dromara.docman.service.IDocProjectTypeService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DocProjectTypeQueryApplicationService implements QueryApplicationService {

    private final IDocProjectTypeService projectTypeService;

    public List<DocProjectTypeVo> listAll() {
        return projectTypeService.listAll();
    }

    public DocProjectTypeVo queryById(Long id) {
        return projectTypeService.queryById(id);
    }
}
