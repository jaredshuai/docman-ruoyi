package org.dromara.docman.application.assembler;

import org.dromara.common.core.application.BaseAssembler;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.docman.domain.entity.DocArchivePackage;
import org.dromara.docman.domain.vo.DocArchivePackageVo;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DocArchiveAssembler implements BaseAssembler<DocArchivePackage, DocArchivePackageVo> {

    @Override
    public DocArchivePackageVo toVo(DocArchivePackage archivePackage) {
        return MapstructUtils.convert(archivePackage, DocArchivePackageVo.class);
    }

    @Override
    public List<DocArchivePackageVo> toVoList(List<DocArchivePackage> archivePackages) {
        return MapstructUtils.convert(archivePackages, DocArchivePackageVo.class);
    }
}
