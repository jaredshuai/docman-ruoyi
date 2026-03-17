package org.dromara.docman.application.service;

import lombok.RequiredArgsConstructor;
import org.dromara.common.core.application.CommandApplicationService;
import org.dromara.docman.application.assembler.DocArchiveAssembler;
import org.dromara.docman.domain.entity.DocArchivePackage;
import org.dromara.docman.domain.vo.DocArchivePackageVo;
import org.dromara.docman.service.IDocArchiveService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DocArchiveApplicationService implements CommandApplicationService {

    private final IDocArchiveService archiveService;
    private final DocArchiveAssembler archiveAssembler;

    public DocArchivePackageVo archive(Long projectId) {
        DocArchivePackage archivePackage = archiveService.archiveProject(projectId);
        return archiveAssembler.toVo(archivePackage);
    }

    public DocArchivePackageVo getLatest(Long projectId) {
        return archiveAssembler.toVo(archiveService.getByProjectId(projectId));
    }

    public List<DocArchivePackageVo> listHistory(Long projectId) {
        return archiveAssembler.toVoList(archiveService.listByProjectId(projectId));
    }
}
