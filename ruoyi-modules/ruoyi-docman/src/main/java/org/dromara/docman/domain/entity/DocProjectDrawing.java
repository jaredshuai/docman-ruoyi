package org.dromara.docman.domain.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.mybatis.core.domain.BaseEntity;

import java.io.Serial;

/**
 * 项目图纸录入表 doc_project_drawing
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("doc_project_drawing")
public class DocProjectDrawing extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id")
    private Long id;

    private Long projectId;

    private String drawingCode;

    private String orderSerialNo;

    private String workContent;

    private Boolean includeInProject;

    private String remark;

    @TableLogic
    private String delFlag;
}
