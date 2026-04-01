package org.dromara.docman.application.service;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.application.CommandApplicationService;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.core.utils.file.FileUtils;
import org.dromara.docman.application.assembler.DocArchiveAssembler;
import org.dromara.docman.domain.entity.DocArchivePackage;
import org.dromara.docman.domain.enums.DocArchiveStatus;
import org.dromara.docman.domain.vo.DocArchivePackageVo;
import org.dromara.docman.service.IDocArchiveService;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DocArchiveApplicationService implements CommandApplicationService {

    private final IDocArchiveService archiveService;
    private final DocArchiveAssembler archiveAssembler;

    /**
     * 发起项目归档并返回归档结果。
     *
     * @param projectId 项目ID
     * @return 归档结果
     */
    public DocArchivePackageVo archive(Long projectId) {
        DocArchivePackage archivePackage = archiveService.archiveProject(projectId);
        return archiveAssembler.toVo(archivePackage);
    }

    /**
     * 查询项目最近一次归档。
     *
     * @param projectId 项目ID
     * @return 最新归档
     */
    public DocArchivePackageVo getLatest(Long projectId) {
        return archiveAssembler.toVo(archiveService.getByProjectId(projectId));
    }

    /**
     * 查询项目归档历史。
     *
     * @param projectId 项目ID
     * @return 历史归档列表
     */
    public List<DocArchivePackageVo> listHistory(Long projectId) {
        return archiveAssembler.toVoList(archiveService.listByProjectId(projectId));
    }

    /**
     * 下载指定归档包。
     *
     * @param archiveId 归档包ID
     * @param response  HTTP响应
     */
    public void downloadArchive(Long archiveId, HttpServletResponse response) {
        DocArchivePackage archivePackage = archiveService.getById(archiveId);
        if (archivePackage == null) {
            throw new ServiceException("归档包不存在");
        }
        if (!DocArchiveStatus.COMPLETED.getCode().equals(archivePackage.getStatus())) {
            throw new ServiceException("归档未完成，无法下载");
        }
        String archivePath = archivePackage.getNasArchivePath();
        if (StringUtils.isBlank(archivePath)) {
            throw new ServiceException("归档文件路径为空");
        }

        Path path = Paths.get(archivePath);
        if (!Files.exists(path) || Files.isDirectory(path)) {
            throw new ServiceException("归档文件不存在: " + archivePath);
        }

        String fileName = buildDownloadFileName(archivePackage);
        FileUtils.setAttachmentResponseHeader(response, fileName);
        response.setContentType(MediaType.parseMediaType("application/zip").toString());
        try {
            response.setContentLengthLong(Files.size(path));
            Files.copy(path, response.getOutputStream());
            response.getOutputStream().flush();
        } catch (IOException e) {
            throw new ServiceException("下载归档包失败");
        }
    }

    private String buildDownloadFileName(DocArchivePackage archivePackage) {
        String archiveNo = archivePackage.getArchiveNo();
        String baseName = StringUtils.isNotBlank(archiveNo) ? archiveNo : "archive-" + archivePackage.getId();
        return baseName.endsWith(".zip") ? baseName : baseName + ".zip";
    }
}
