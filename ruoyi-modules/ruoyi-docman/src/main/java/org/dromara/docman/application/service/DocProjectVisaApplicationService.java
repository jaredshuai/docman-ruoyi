package org.dromara.docman.application.service;

import lombok.RequiredArgsConstructor;
import org.dromara.common.core.application.CommandApplicationService;
import org.dromara.docman.domain.bo.DocProjectVisaBo;
import org.dromara.docman.service.IDocProjectVisaService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DocProjectVisaApplicationService implements CommandApplicationService {

    private final IDocProjectVisaService visaService;

    public Long save(DocProjectVisaBo bo) {
        return visaService.save(bo);
    }

    public void delete(List<Long> ids) {
        visaService.deleteByIds(ids);
    }
}
