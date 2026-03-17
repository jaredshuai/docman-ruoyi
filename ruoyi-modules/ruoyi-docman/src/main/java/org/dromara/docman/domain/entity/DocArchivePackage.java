package org.dromara.docman.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.mybatis.core.domain.BaseEntity;

import java.io.Serial;
import java.util.List;
import java.util.Map;

/**
 * 归档包表 doc_archive_package
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "doc_archive_package", autoResultMap = true)
public class DocArchivePackage extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id")
    private Long id;

    private Long projectId;

    private String archiveNo;

    private Long archiveVersion;

    private String nasArchivePath;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<Map<String, String>> manifest;

    private String snapshotChecksum;

    private java.util.Date requestedAt;

    private java.util.Date completedAt;

    /** 状态（requested/generating/completed/failed） */
    private String status;
}
