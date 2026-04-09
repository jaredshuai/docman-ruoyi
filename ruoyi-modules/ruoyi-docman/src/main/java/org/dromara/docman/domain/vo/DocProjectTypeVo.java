package org.dromara.docman.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 项目类型视图对象
 */
@Data
public class DocProjectTypeVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private String code;
    private String name;
    private String customerType;
    private String description;
    private Integer sortOrder;
    private String status;
    private Date createTime;
    private Date updateTime;
}
