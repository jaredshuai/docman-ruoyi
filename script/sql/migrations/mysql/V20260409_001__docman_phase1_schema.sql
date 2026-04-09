SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DROP PROCEDURE IF EXISTS ensure_column;
DROP PROCEDURE IF EXISTS ensure_index;

DELIMITER $$

CREATE PROCEDURE ensure_column(IN p_table VARCHAR(128), IN p_column VARCHAR(128), IN p_ddl TEXT)
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = p_table
          AND column_name = p_column
    ) THEN
        SET @ddl = p_ddl;
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$

CREATE PROCEDURE ensure_index(IN p_table VARCHAR(128), IN p_index VARCHAR(128), IN p_ddl TEXT)
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = p_table
          AND index_name = p_index
    ) THEN
        SET @ddl = p_ddl;
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$

DELIMITER ;

CREATE TABLE IF NOT EXISTS `doc_project` (
    `id` BIGINT NOT NULL COMMENT '项目ID',
    `name` VARCHAR(200) NOT NULL COMMENT '项目名称',
    `project_type_code` VARCHAR(64) NULL DEFAULT NULL COMMENT '项目类型编码',
    `customer_type` VARCHAR(20) NOT NULL COMMENT '客户类型（telecom/social）',
    `business_type` VARCHAR(20) NOT NULL COMMENT '业务类型（pipeline/weak_current）',
    `document_category` VARCHAR(20) NOT NULL COMMENT '文档类别（telecom/internal/customer）',
    `status` VARCHAR(20) NOT NULL DEFAULT 'active' COMMENT '项目状态（active/archived）',
    `owner_id` BIGINT NOT NULL COMMENT '负责人ID',
    `nas_base_path` VARCHAR(500) NULL DEFAULT NULL COMMENT '群晖NAS基础路径',
    `nas_dir_status` VARCHAR(20) NOT NULL DEFAULT 'pending' COMMENT 'NAS目录状态（pending/created/failed）',
    `remark` VARCHAR(500) NULL DEFAULT NULL COMMENT '备注',
    `create_dept` BIGINT NULL DEFAULT NULL COMMENT '创建部门',
    `create_by` BIGINT NULL DEFAULT NULL COMMENT '创建者',
    `create_time` DATETIME NULL DEFAULT NULL COMMENT '创建时间',
    `update_by` BIGINT NULL DEFAULT NULL COMMENT '更新者',
    `update_time` DATETIME NULL DEFAULT NULL COMMENT '更新时间',
    `del_flag` CHAR(1) NULL DEFAULT '0' COMMENT '删除标志（0正常 1删除）',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='项目表';

CALL ensure_column('doc_project', 'dianxin_code',
    'ALTER TABLE `doc_project` ADD COLUMN `dianxin_code` VARCHAR(200) NULL DEFAULT NULL COMMENT ''电信编号'' AFTER `name`');
CALL ensure_column('doc_project', 'xiangyun_code',
    'ALTER TABLE `doc_project` ADD COLUMN `xiangyun_code` VARCHAR(200) NULL DEFAULT NULL COMMENT ''翔云编号'' AFTER `dianxin_code`');
CALL ensure_column('doc_project', 'price',
    'ALTER TABLE `doc_project` ADD COLUMN `price` DECIMAL(10,2) NULL DEFAULT NULL COMMENT ''项目金额'' AFTER `xiangyun_code`');
CALL ensure_column('doc_project', 'project_type_code',
    'ALTER TABLE `doc_project` ADD COLUMN `project_type_code` VARCHAR(64) NULL DEFAULT NULL COMMENT ''项目类型编码'' AFTER `price`');
CALL ensure_column('doc_project', 'customer_name',
    'ALTER TABLE `doc_project` ADD COLUMN `customer_name` VARCHAR(200) NULL DEFAULT NULL COMMENT ''客户名称'' AFTER `status`');
CALL ensure_column('doc_project', 'dianxin_initiation_time',
    'ALTER TABLE `doc_project` ADD COLUMN `dianxin_initiation_time` DATETIME NULL DEFAULT NULL COMMENT ''电信立项时间'' AFTER `owner_id`');
CALL ensure_column('doc_project', 'start_time',
    'ALTER TABLE `doc_project` ADD COLUMN `start_time` DATETIME NULL DEFAULT NULL COMMENT ''计划开工时间'' AFTER `dianxin_initiation_time`');
CALL ensure_column('doc_project', 'end_time',
    'ALTER TABLE `doc_project` ADD COLUMN `end_time` DATETIME NULL DEFAULT NULL COMMENT ''计划完工时间'' AFTER `start_time`');

CALL ensure_index('doc_project', 'idx_project_owner',
    'ALTER TABLE `doc_project` ADD INDEX `idx_project_owner` (`owner_id`)');
CALL ensure_index('doc_project', 'idx_project_status',
    'ALTER TABLE `doc_project` ADD INDEX `idx_project_status` (`status`)');
CALL ensure_index('doc_project', 'idx_project_list',
    'ALTER TABLE `doc_project` ADD INDEX `idx_project_list` (`create_time`, `customer_type`, `business_type`)');

CREATE TABLE IF NOT EXISTS `doc_project_add_record` (
    `id` BIGINT NOT NULL COMMENT '增加工作量记录id',
    `project_id` BIGINT NOT NULL COMMENT '项目id',
    `enable` TINYINT NULL DEFAULT NULL COMMENT '是否启用',
    `estimated_price` DECIMAL(20,5) NULL DEFAULT NULL COMMENT '预估价格',
    `remark` VARCHAR(500) NULL DEFAULT NULL COMMENT '备注',
    `create_dept` BIGINT NULL DEFAULT NULL COMMENT '创建部门',
    `create_by` BIGINT NULL DEFAULT NULL COMMENT '创建者',
    `create_time` DATETIME NULL DEFAULT NULL COMMENT '创建时间',
    `update_by` BIGINT NULL DEFAULT NULL COMMENT '更新者',
    `update_time` DATETIME NULL DEFAULT NULL COMMENT '更新时间',
    `del_flag` CHAR(1) NULL DEFAULT '0' COMMENT '删除标志（0正常 1删除）',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='项目工作量记录表';

CALL ensure_index('doc_project_add_record', 'idx_project_add_record_project',
    'ALTER TABLE `doc_project_add_record` ADD INDEX `idx_project_add_record_project` (`project_id`)');

CREATE TABLE IF NOT EXISTS `doc_project_add_record_detail` (
    `id` BIGINT NOT NULL COMMENT '工作量id',
    `project_id` BIGINT NOT NULL COMMENT '项目id',
    `project_add_record_id` BIGINT NOT NULL COMMENT '增加工作量记录id',
    `name` VARCHAR(255) NULL DEFAULT NULL COMMENT '工作量名称',
    `alias` VARCHAR(255) NULL DEFAULT NULL COMMENT '别名',
    `price` DECIMAL(10,5) NULL DEFAULT NULL COMMENT '价格',
    `remark` VARCHAR(500) NULL DEFAULT NULL COMMENT '备注',
    `create_dept` BIGINT NULL DEFAULT NULL COMMENT '创建部门',
    `create_by` BIGINT NULL DEFAULT NULL COMMENT '创建者',
    `create_time` DATETIME NULL DEFAULT NULL COMMENT '创建时间',
    `update_by` BIGINT NULL DEFAULT NULL COMMENT '更新者',
    `update_time` DATETIME NULL DEFAULT NULL COMMENT '更新时间',
    `del_flag` CHAR(1) NULL DEFAULT '0' COMMENT '删除标志（0正常 1删除）',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='项目工作量记录详情表';

CALL ensure_index('doc_project_add_record_detail', 'idx_project_add_record_detail_project',
    'ALTER TABLE `doc_project_add_record_detail` ADD INDEX `idx_project_add_record_detail_project` (`project_id`)');
CALL ensure_index('doc_project_add_record_detail', 'idx_project_add_record_detail_record',
    'ALTER TABLE `doc_project_add_record_detail` ADD INDEX `idx_project_add_record_detail_record` (`project_add_record_id`)');

CREATE TABLE IF NOT EXISTS `doc_project_type` (
    `id` BIGINT NOT NULL COMMENT '项目类型ID',
    `code` VARCHAR(64) NOT NULL COMMENT '项目类型编码',
    `name` VARCHAR(128) NOT NULL COMMENT '项目类型名称',
    `customer_type` VARCHAR(20) NOT NULL COMMENT '客户类型',
    `description` VARCHAR(500) NULL DEFAULT NULL COMMENT '说明',
    `sort_order` INT NULL DEFAULT 0 COMMENT '排序',
    `status` VARCHAR(20) NOT NULL DEFAULT 'active' COMMENT '状态（active/inactive）',
    `create_dept` BIGINT NULL DEFAULT NULL COMMENT '创建部门',
    `create_by` BIGINT NULL DEFAULT NULL COMMENT '创建者',
    `create_time` DATETIME NULL DEFAULT NULL COMMENT '创建时间',
    `update_by` BIGINT NULL DEFAULT NULL COMMENT '更新者',
    `update_time` DATETIME NULL DEFAULT NULL COMMENT '更新时间',
    `del_flag` CHAR(1) NULL DEFAULT '0' COMMENT '删除标志（0正常 1删除）',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='项目类型定义表';

CALL ensure_index('doc_project_type', 'uk_project_type_code',
    'ALTER TABLE `doc_project_type` ADD UNIQUE KEY `uk_project_type_code` (`code`)');
CALL ensure_index('doc_project_type', 'idx_project_type_status',
    'ALTER TABLE `doc_project_type` ADD INDEX `idx_project_type_status` (`status`)');

INSERT INTO `doc_project_type` (
    `id`, `code`, `name`, `customer_type`, `description`, `sort_order`, `status`,
    `create_dept`, `create_by`, `create_time`, `del_flag`
)
SELECT 9101001, 'telecom', '电信项目', 'telecom', '电信项目默认类型', 1, 'active', 103, 1, NOW(), '0'
WHERE NOT EXISTS (
    SELECT 1 FROM `doc_project_type` WHERE `code` = 'telecom'
);

CREATE TABLE IF NOT EXISTS `doc_workflow_template` (
    `id` BIGINT NOT NULL COMMENT '工作流模板ID',
    `code` VARCHAR(64) NOT NULL COMMENT '模板编码',
    `name` VARCHAR(128) NOT NULL COMMENT '模板名称',
    `project_type_code` VARCHAR(64) NOT NULL COMMENT '项目类型编码',
    `description` VARCHAR(500) NULL DEFAULT NULL COMMENT '说明',
    `default_flag` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否默认模板',
    `sort_order` INT NULL DEFAULT 0 COMMENT '排序',
    `status` VARCHAR(20) NOT NULL DEFAULT 'active' COMMENT '状态（active/inactive）',
    `create_dept` BIGINT NULL DEFAULT NULL COMMENT '创建部门',
    `create_by` BIGINT NULL DEFAULT NULL COMMENT '创建者',
    `create_time` DATETIME NULL DEFAULT NULL COMMENT '创建时间',
    `update_by` BIGINT NULL DEFAULT NULL COMMENT '更新者',
    `update_time` DATETIME NULL DEFAULT NULL COMMENT '更新时间',
    `del_flag` CHAR(1) NULL DEFAULT '0' COMMENT '删除标志（0正常 1删除）',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工作流模板表';

CALL ensure_index('doc_workflow_template', 'uk_workflow_template_code',
    'ALTER TABLE `doc_workflow_template` ADD UNIQUE KEY `uk_workflow_template_code` (`code`)');
CALL ensure_index('doc_workflow_template', 'idx_workflow_template_project_type',
    'ALTER TABLE `doc_workflow_template` ADD INDEX `idx_workflow_template_project_type` (`project_type_code`)');

CREATE TABLE IF NOT EXISTS `doc_workflow_template_node` (
    `id` BIGINT NOT NULL COMMENT '模板节点ID',
    `template_id` BIGINT NOT NULL COMMENT '模板ID',
    `node_code` VARCHAR(64) NOT NULL COMMENT '节点编码',
    `node_name` VARCHAR(128) NOT NULL COMMENT '节点名称',
    `sort_order` INT NULL DEFAULT 0 COMMENT '排序',
    `description` VARCHAR(500) NULL DEFAULT NULL COMMENT '说明',
    `status` VARCHAR(20) NOT NULL DEFAULT 'active' COMMENT '状态（active/inactive）',
    `create_dept` BIGINT NULL DEFAULT NULL COMMENT '创建部门',
    `create_by` BIGINT NULL DEFAULT NULL COMMENT '创建者',
    `create_time` DATETIME NULL DEFAULT NULL COMMENT '创建时间',
    `update_by` BIGINT NULL DEFAULT NULL COMMENT '更新者',
    `update_time` DATETIME NULL DEFAULT NULL COMMENT '更新时间',
    `del_flag` CHAR(1) NULL DEFAULT '0' COMMENT '删除标志（0正常 1删除）',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工作流模板节点表';

CALL ensure_index('doc_workflow_template_node', 'idx_workflow_node_template',
    'ALTER TABLE `doc_workflow_template_node` ADD INDEX `idx_workflow_node_template` (`template_id`)');

CREATE TABLE IF NOT EXISTS `doc_workflow_node_task` (
    `id` BIGINT NOT NULL COMMENT '节点事项ID',
    `node_id` BIGINT NOT NULL COMMENT '模板节点ID',
    `task_code` VARCHAR(64) NOT NULL COMMENT '事项编码',
    `task_name` VARCHAR(128) NOT NULL COMMENT '事项名称',
    `task_type` VARCHAR(32) NOT NULL COMMENT '事项类型',
    `required_flag` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否必需',
    `sort_order` INT NULL DEFAULT 0 COMMENT '排序',
    `completion_rule` VARCHAR(255) NULL DEFAULT NULL COMMENT '完成规则表达式',
    `plugin_codes` VARCHAR(500) NULL DEFAULT NULL COMMENT '关联插件编码列表',
    `description` VARCHAR(500) NULL DEFAULT NULL COMMENT '说明',
    `status` VARCHAR(20) NOT NULL DEFAULT 'active' COMMENT '状态（active/inactive）',
    `create_dept` BIGINT NULL DEFAULT NULL COMMENT '创建部门',
    `create_by` BIGINT NULL DEFAULT NULL COMMENT '创建者',
    `create_time` DATETIME NULL DEFAULT NULL COMMENT '创建时间',
    `update_by` BIGINT NULL DEFAULT NULL COMMENT '更新者',
    `update_time` DATETIME NULL DEFAULT NULL COMMENT '更新时间',
    `del_flag` CHAR(1) NULL DEFAULT '0' COMMENT '删除标志（0正常 1删除）',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工作流节点事项表';

CALL ensure_index('doc_workflow_node_task', 'idx_workflow_task_node',
    'ALTER TABLE `doc_workflow_node_task` ADD INDEX `idx_workflow_task_node` (`node_id`)');

CREATE TABLE IF NOT EXISTS `doc_project_drawing` (
    `id` BIGINT NOT NULL COMMENT '图纸记录ID',
    `project_id` BIGINT NOT NULL COMMENT '项目ID',
    `drawing_code` VARCHAR(128) NULL DEFAULT NULL COMMENT '图纸编码/图号',
    `order_serial_no` VARCHAR(128) NULL DEFAULT NULL COMMENT '订单流水号',
    `work_content` VARCHAR(1000) NULL DEFAULT NULL COMMENT '工作内容',
    `include_in_project` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否计入当前项目',
    `remark` VARCHAR(500) NULL DEFAULT NULL COMMENT '备注',
    `create_dept` BIGINT NULL DEFAULT NULL COMMENT '创建部门',
    `create_by` BIGINT NULL DEFAULT NULL COMMENT '创建者',
    `create_time` DATETIME NULL DEFAULT NULL COMMENT '创建时间',
    `update_by` BIGINT NULL DEFAULT NULL COMMENT '更新者',
    `update_time` DATETIME NULL DEFAULT NULL COMMENT '更新时间',
    `del_flag` CHAR(1) NULL DEFAULT '0' COMMENT '删除标志（0正常 1删除）',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='项目图纸录入表';

CALL ensure_index('doc_project_drawing', 'idx_project_drawing_project',
    'ALTER TABLE `doc_project_drawing` ADD INDEX `idx_project_drawing_project` (`project_id`)');

CREATE TABLE IF NOT EXISTS `doc_project_visa` (
    `id` BIGINT NOT NULL COMMENT '签证记录ID',
    `project_id` BIGINT NOT NULL COMMENT '项目ID',
    `reason` VARCHAR(500) NULL DEFAULT NULL COMMENT '签证原因',
    `content_basis` VARCHAR(2000) NULL DEFAULT NULL COMMENT '签证内容及依据',
    `amount` DECIMAL(12,2) NULL DEFAULT NULL COMMENT '签证金额',
    `visa_date` DATETIME NULL DEFAULT NULL COMMENT '签证日期',
    `include_in_project` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否计入当前项目',
    `remark` VARCHAR(500) NULL DEFAULT NULL COMMENT '备注',
    `create_dept` BIGINT NULL DEFAULT NULL COMMENT '创建部门',
    `create_by` BIGINT NULL DEFAULT NULL COMMENT '创建者',
    `create_time` DATETIME NULL DEFAULT NULL COMMENT '创建时间',
    `update_by` BIGINT NULL DEFAULT NULL COMMENT '更新者',
    `update_time` DATETIME NULL DEFAULT NULL COMMENT '更新时间',
    `del_flag` CHAR(1) NULL DEFAULT '0' COMMENT '删除标志（0正常 1删除）',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='项目签证录入表';

CALL ensure_index('doc_project_visa', 'idx_project_visa_project',
    'ALTER TABLE `doc_project_visa` ADD INDEX `idx_project_visa_project` (`project_id`)');

CREATE TABLE IF NOT EXISTS `doc_project_order` (
    `id` BIGINT NOT NULL COMMENT '项目签证单ID',
    `project_id` BIGINT NOT NULL COMMENT '项目id',
    `reason` VARCHAR(255) NULL DEFAULT NULL COMMENT '事由',
    `date` DATE NULL DEFAULT NULL COMMENT '日期',
    `amount` DECIMAL(10,2) NULL DEFAULT NULL COMMENT '金额',
    `remark` VARCHAR(255) NULL DEFAULT NULL COMMENT '备注',
    `create_dept` BIGINT NULL DEFAULT NULL COMMENT '创建部门',
    `create_by` BIGINT NULL DEFAULT NULL COMMENT '创建者',
    `create_time` DATETIME NULL DEFAULT NULL COMMENT '创建时间',
    `update_by` BIGINT NULL DEFAULT NULL COMMENT '更新者',
    `update_time` DATETIME NULL DEFAULT NULL COMMENT '更新时间',
    `del_flag` CHAR(1) NULL DEFAULT '0' COMMENT '删除标志（0正常 1删除）',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='项目签证单表';

CALL ensure_index('doc_project_order', 'idx_project_order_project',
    'ALTER TABLE `doc_project_order` ADD INDEX `idx_project_order_project` (`project_id`)');

CREATE TABLE IF NOT EXISTS `doc_project_runtime` (
    `id` BIGINT NOT NULL COMMENT '项目运行时ID',
    `project_id` BIGINT NOT NULL COMMENT '项目ID',
    `workflow_template_id` BIGINT NOT NULL COMMENT '工作流模板ID',
    `current_node_code` VARCHAR(64) NOT NULL COMMENT '当前节点编码',
    `status` VARCHAR(20) NOT NULL DEFAULT 'running' COMMENT '运行状态（running/completed）',
    `create_dept` BIGINT NULL DEFAULT NULL COMMENT '创建部门',
    `create_by` BIGINT NULL DEFAULT NULL COMMENT '创建者',
    `create_time` DATETIME NULL DEFAULT NULL COMMENT '创建时间',
    `update_by` BIGINT NULL DEFAULT NULL COMMENT '更新者',
    `update_time` DATETIME NULL DEFAULT NULL COMMENT '更新时间',
    `del_flag` CHAR(1) NULL DEFAULT '0' COMMENT '删除标志（0正常 1删除）',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='项目流程运行时表';

CALL ensure_index('doc_project_runtime', 'uk_project_runtime_project',
    'ALTER TABLE `doc_project_runtime` ADD UNIQUE KEY `uk_project_runtime_project` (`project_id`)');

CREATE TABLE IF NOT EXISTS `doc_project_node_task_runtime` (
    `id` BIGINT NOT NULL COMMENT '节点事项运行时ID',
    `project_id` BIGINT NOT NULL COMMENT '项目ID',
    `node_code` VARCHAR(64) NOT NULL COMMENT '节点编码',
    `task_code` VARCHAR(64) NOT NULL COMMENT '事项编码',
    `status` VARCHAR(20) NOT NULL DEFAULT 'pending' COMMENT '状态（pending/completed/skipped）',
    `completed_by` BIGINT NULL DEFAULT NULL COMMENT '完成人',
    `completed_at` DATETIME NULL DEFAULT NULL COMMENT '完成时间',
    `evidence_ref` VARCHAR(500) NULL DEFAULT NULL COMMENT '完成凭据引用',
    `create_dept` BIGINT NULL DEFAULT NULL COMMENT '创建部门',
    `create_by` BIGINT NULL DEFAULT NULL COMMENT '创建者',
    `create_time` DATETIME NULL DEFAULT NULL COMMENT '创建时间',
    `update_by` BIGINT NULL DEFAULT NULL COMMENT '更新者',
    `update_time` DATETIME NULL DEFAULT NULL COMMENT '更新时间',
    `del_flag` CHAR(1) NULL DEFAULT '0' COMMENT '删除标志（0正常 1删除）',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='项目节点事项运行时表';

CALL ensure_index('doc_project_node_task_runtime', 'idx_project_task_runtime_project_node',
    'ALTER TABLE `doc_project_node_task_runtime` ADD INDEX `idx_project_task_runtime_project_node` (`project_id`, `node_code`)');

CREATE TABLE IF NOT EXISTS `doc_project_estimate_snapshot` (
    `id` BIGINT NOT NULL COMMENT '估算快照ID',
    `project_id` BIGINT NOT NULL COMMENT '项目ID',
    `estimate_type` VARCHAR(64) NOT NULL DEFAULT 'initial_estimate' COMMENT '估算类型',
    `estimate_amount` DECIMAL(18,2) NOT NULL COMMENT '估算金额',
    `drawing_count` BIGINT NOT NULL DEFAULT 0 COMMENT '计入项目的图纸数量',
    `visa_count` BIGINT NOT NULL DEFAULT 0 COMMENT '计入项目的签证数量',
    `status` VARCHAR(32) NOT NULL DEFAULT 'mocked' COMMENT '估算状态',
    `summary` VARCHAR(2000) NULL DEFAULT NULL COMMENT '估算摘要',
    `create_dept` BIGINT NULL DEFAULT NULL COMMENT '创建部门',
    `create_by` BIGINT NULL DEFAULT NULL COMMENT '创建者',
    `create_time` DATETIME NULL DEFAULT NULL COMMENT '创建时间',
    `update_by` BIGINT NULL DEFAULT NULL COMMENT '更新者',
    `update_time` DATETIME NULL DEFAULT NULL COMMENT '更新时间',
    `del_flag` CHAR(1) NULL DEFAULT '0' COMMENT '删除标志（0正常 1删除）',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='项目估算结果快照表';

CALL ensure_index('doc_project_estimate_snapshot', 'idx_project_estimate_snapshot_project',
    'ALTER TABLE `doc_project_estimate_snapshot` ADD INDEX `idx_project_estimate_snapshot_project` (`project_id`)');

CREATE TABLE IF NOT EXISTS `doc_project_balance_adjustment` (
    `id` BIGINT NOT NULL COMMENT '平料记录ID',
    `project_id` BIGINT NOT NULL COMMENT '项目ID',
    `material_price` DECIMAL(18,2) NULL DEFAULT NULL COMMENT '材料价格',
    `balance_remark` VARCHAR(1000) NULL DEFAULT NULL COMMENT '平料备注',
    `status` VARCHAR(20) NOT NULL DEFAULT 'active' COMMENT '状态',
    `create_dept` BIGINT NULL DEFAULT NULL COMMENT '创建部门',
    `create_by` BIGINT NULL DEFAULT NULL COMMENT '创建者',
    `create_time` DATETIME NULL DEFAULT NULL COMMENT '创建时间',
    `update_by` BIGINT NULL DEFAULT NULL COMMENT '更新者',
    `update_time` DATETIME NULL DEFAULT NULL COMMENT '更新时间',
    `del_flag` CHAR(1) NULL DEFAULT '0' COMMENT '删除标志（0正常 1删除）',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='项目平料与价格调整表';

CALL ensure_index('doc_project_balance_adjustment', 'idx_project_balance_adjustment_project',
    'ALTER TABLE `doc_project_balance_adjustment` ADD INDEX `idx_project_balance_adjustment_project` (`project_id`, `create_time`)');

DROP PROCEDURE IF EXISTS ensure_column;
DROP PROCEDURE IF EXISTS ensure_index;

SET FOREIGN_KEY_CHECKS = 1;
