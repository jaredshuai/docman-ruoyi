package org.dromara.docman.domain.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 项目成员关联表 doc_project_member
 */
@Data
@TableName("doc_project_member")
public class DocProjectMember implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id")
    private Long id;

    private Long projectId;

    private Long userId;

    /** 角色类型（owner/editor/viewer） */
    private String roleType;

    private Date createTime;
}
