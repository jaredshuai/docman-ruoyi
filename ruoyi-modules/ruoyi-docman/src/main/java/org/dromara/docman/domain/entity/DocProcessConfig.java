package org.dromara.docman.domain.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.mybatis.core.domain.BaseEntity;

import java.io.Serial;

/**
 * 流程-项目绑定配置表 doc_process_config
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("doc_process_config")
public class DocProcessConfig extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id")
    private Long id;

    private Long projectId;

    /** Warm-Flow 流程定义ID */
    private Long definitionId;

    /** Warm-Flow 流程实例ID（启动后填入） */
    private Long instanceId;

    /** 状态（pending/running/completed） */
    private String status;
}
