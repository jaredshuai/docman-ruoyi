package org.dromara.docman.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 项目签证录入视图对象
 */
@Data
public class DocProjectVisaVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long projectId;
    private String reason;
    private String contentBasis;
    private BigDecimal amount;
    private Date visaDate;
    private Boolean includeInProject;
    private String remark;
    private Date createTime;
    private Date updateTime;
}
