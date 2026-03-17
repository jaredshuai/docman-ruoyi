package org.dromara.docman.domain.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.mybatis.core.domain.BaseEntity;

import java.io.Serial;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("doc_plugin_execution_log")
public class DocPluginExecutionLog extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id")
    private Long id;

    private Long projectId;

    private Long processInstanceId;

    private String nodeCode;

    private String pluginId;

    private String pluginName;

    private String status;

    private Long costMs;

    private Integer generatedFileCount;

    private String errorMessage;

    private String requestSnapshot;

    private String resultSnapshot;
}
