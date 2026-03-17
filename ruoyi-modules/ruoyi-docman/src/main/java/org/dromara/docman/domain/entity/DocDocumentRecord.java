package org.dromara.docman.domain.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.mybatis.core.domain.BaseEntity;

import java.io.Serial;
import java.util.Date;

/**
 * 文档记录表 doc_document_record
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("doc_document_record")
public class DocDocumentRecord extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id")
    private Long id;

    private Long projectId;

    /** 所属节点实例（Plan B 中使用） */
    private Long nodeInstanceId;

    /** 生成来源插件ID（手动上传为空） */
    private String pluginId;

    /** 来源类型（plugin/upload） */
    private String sourceType;

    /** 中文可读文件名 */
    private String fileName;

    /** 群晖完整路径 */
    private String nasPath;

    /** 关联 sys_oss 表ID */
    private Long ossId;

    /** 状态（pending/running/generated/failed/archived/obsolete） */
    private String status;

    private Date generatedAt;

    private Date archivedAt;

    @TableLogic
    private String delFlag;
}
