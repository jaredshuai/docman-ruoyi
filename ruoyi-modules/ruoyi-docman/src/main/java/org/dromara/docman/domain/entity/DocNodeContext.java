package org.dromara.docman.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * 节点上下文表 doc_node_context
 */
@Data
@TableName(value = "doc_node_context", autoResultMap = true)
public class DocNodeContext implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id")
    private Long id;

    private Long processInstanceId;

    private String nodeCode;

    private Long projectId;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> processVariables;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> nodeVariables;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> documentFacts;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, String> unstructuredContent;

    private Date createTime;

    private Date updateTime;

    public static DocNodeContext create(Long processInstanceId, String nodeCode, Long projectId) {
        DocNodeContext ctx = new DocNodeContext();
        ctx.setProcessInstanceId(processInstanceId);
        ctx.setNodeCode(nodeCode);
        ctx.setProjectId(projectId);
        ctx.setProcessVariables(new HashMap<>());
        ctx.setNodeVariables(new HashMap<>());
        ctx.setDocumentFacts(new HashMap<>());
        ctx.setUnstructuredContent(new HashMap<>());
        ctx.setCreateTime(new Date());
        ctx.setUpdateTime(new Date());
        return ctx;
    }
}
