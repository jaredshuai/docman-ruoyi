package org.dromara.docman.domain.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.mybatis.core.domain.BaseEntity;

import java.io.Serial;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 项目表 doc_project
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("doc_project")
public class DocProject extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id")
    private Long id;

    /** 项目名称 */
    private String name;

    /** 电信编号 */
    private String dianxinCode;

    /** 翔云编号 */
    private String xiangyunCode;

    /** 项目金额 */
    private java.math.BigDecimal price;

    /** 客户类型（telecom/social） */
    private String customerType;

    /** 业务类型（pipeline/weak_current） */
    private String businessType;

    /** 文档类别（telecom/internal/customer） */
    private String documentCategory;

    /** 项目状态（active/archived） */
    private String status;

    /** 客户名称 */
    private String customerName;

    /** 负责人ID */
    private Long ownerId;

    /** 电信立项时间 */
    private Date dianxinInitiationTime;

    /** 计划开工时间 */
    private Date startTime;

    /** 计划完工时间 */
    private Date endTime;

    /** 群晖NAS基础路径 */
    private String nasBasePath;

    /** NAS目录状态（pending/created/failed） */
    private String nasDirStatus;

    /** 备注 */
    private String remark;

    @TableLogic
    private String delFlag;
}
