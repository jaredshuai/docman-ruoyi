package org.dromara.docman.application.assembler;

import org.dromara.common.core.application.BaseAssembler;
import org.dromara.docman.domain.entity.DocArchivePackage;
import org.dromara.docman.domain.vo.DocArchivePackageVo;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class DocArchiveAssembler implements BaseAssembler<DocArchivePackage, DocArchivePackageVo> {

    @Override
    public DocArchivePackageVo toVo(DocArchivePackage archivePackage) {
        if (archivePackage == null) {
            return null;
        }
        DocArchivePackageVo vo = new DocArchivePackageVo();
        vo.setId(archivePackage.getId());
        vo.setProjectId(archivePackage.getProjectId());
        vo.setArchiveNo(archivePackage.getArchiveNo());
        vo.setArchiveVersion(archivePackage.getArchiveVersion());
        vo.setNasArchivePath(archivePackage.getNasArchivePath());
        vo.setManifest(copyManifest(archivePackage.getManifest()));
        vo.setSnapshotChecksum(archivePackage.getSnapshotChecksum());
        vo.setRequestedAt(archivePackage.getRequestedAt());
        vo.setCompletedAt(archivePackage.getCompletedAt());
        vo.setStatus(archivePackage.getStatus());
        vo.setCreateTime(archivePackage.getCreateTime());
        vo.setUpdateTime(archivePackage.getUpdateTime());
        return vo;
    }

    @Override
    public List<DocArchivePackageVo> toVoList(List<DocArchivePackage> archivePackages) {
        if (archivePackages == null || archivePackages.isEmpty()) {
            return List.of();
        }
        return archivePackages.stream().map(this::toVo).toList();
    }

    private List<Map<String, String>> copyManifest(List<Map<String, String>> manifest) {
        if (manifest == null) {
            return null;
        }
        return new ArrayList<>(manifest);
    }
}
