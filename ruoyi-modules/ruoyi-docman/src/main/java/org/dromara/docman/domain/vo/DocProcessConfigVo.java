package org.dromara.docman.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.docman.domain.entity.DocProcessConfig;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

@Data
@AutoMapper(target = DocProcessConfig.class)
public class DocProcessConfigVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long projectId;
    private Long definitionId;
    private Long instanceId;
    private String status;
    private Date createTime;
    private Date updateTime;
}
