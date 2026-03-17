package org.dromara.docman.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.docman.domain.entity.DocArchivePackage;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Data
@AutoMapper(target = DocArchivePackage.class)
public class DocArchivePackageVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long projectId;
    private String archiveNo;
    private Long archiveVersion;
    private String nasArchivePath;
    private List<Map<String, String>> manifest;
    private String snapshotChecksum;
    private Date requestedAt;
    private Date completedAt;
    private String status;
    private Date createTime;
    private Date updateTime;
}
