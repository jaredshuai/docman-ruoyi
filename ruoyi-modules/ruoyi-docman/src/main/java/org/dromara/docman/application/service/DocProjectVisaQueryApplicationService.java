package org.dromara.docman.application.service;

import lombok.RequiredArgsConstructor;
import org.dromara.common.core.application.QueryApplicationService;
import org.dromara.docman.domain.vo.DocProjectVisaVo;
import org.dromara.docman.service.IDocProjectVisaService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DocProjectVisaQueryApplicationService implements QueryApplicationService {

    private final IDocProjectVisaService visaService;

    public List<DocProjectVisaVo> listByProject(Long projectId) {
        return visaService.listByProject(projectId);
    }

    public DocProjectVisaVo queryById(Long id) {
        return visaService.queryById(id);
    }
}
