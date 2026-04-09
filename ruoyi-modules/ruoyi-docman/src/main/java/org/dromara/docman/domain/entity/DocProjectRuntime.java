package org.dromara.docman.domain.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.mybatis.core.domain.BaseEntity;

import java.io.Serial;

/**
 * 项目流程运行时表 doc_project_runtime
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("doc_project_runtime")
public class DocProjectRuntime extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id")
    private Long id;

    private Long projectId;

    private Long workflowTemplateId;

    private String currentNodeCode;

    private String status;

    @TableLogic
    private String delFlag;
}
