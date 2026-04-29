SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DROP PROCEDURE IF EXISTS ensure_column;
DROP PROCEDURE IF EXISTS ensure_index;
DROP PROCEDURE IF EXISTS relax_legacy_column;

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

CREATE PROCEDURE relax_legacy_column(IN p_table VARCHAR(128), IN p_column VARCHAR(128), IN p_ddl TEXT)
BEGIN
    IF EXISTS (
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

DELIMITER ;

CREATE TABLE IF NOT EXISTS `doc_project_drawing_work_item` (
    `id` BIGINT NOT NULL COMMENT '图纸工作量ID',
    `project_id` BIGINT NOT NULL COMMENT '项目ID',
    `tenant_id` VARCHAR(20) NOT NULL DEFAULT '000000' COMMENT '租户编号',
    `drawing_id` BIGINT NOT NULL COMMENT '图纸ID',
    `work_item_name` VARCHAR(255) NOT NULL COMMENT '工作量名称',
    `technician` DECIMAL(20,5) NOT NULL DEFAULT 0 COMMENT '技工',
    `technician_coefficient` DECIMAL(20,5) NOT NULL DEFAULT 1 COMMENT '技工系数',
    `general_worker` DECIMAL(20,5) NOT NULL DEFAULT 0 COMMENT '普工',
    `general_worker_coefficient` DECIMAL(20,5) NOT NULL DEFAULT 1 COMMENT '普工系数',
    `machine_shift` DECIMAL(20,5) NOT NULL DEFAULT 0 COMMENT '机械台班',
    `machine_shift_unit_price` DECIMAL(20,5) NOT NULL DEFAULT 0 COMMENT '机械台班单价',
    `machine_shift_coefficient` DECIMAL(20,5) NOT NULL DEFAULT 1 COMMENT '机械台班系数',
    `instrument_shift` DECIMAL(20,5) NOT NULL DEFAULT 0 COMMENT '仪器仪表台班',
    `instrument_shift_unit_price` DECIMAL(20,5) NOT NULL DEFAULT 0 COMMENT '仪器仪表台班单价',
    `instrument_shift_coefficient` DECIMAL(20,5) NOT NULL DEFAULT 1 COMMENT '仪器仪表系数',
    `material_quantity` DECIMAL(20,5) NOT NULL DEFAULT 0 COMMENT '材料数量',
    `material_unit_price` DECIMAL(20,5) NOT NULL DEFAULT 0 COMMENT '材料单价',
    `create_dept` BIGINT NULL DEFAULT NULL COMMENT '创建部门',
    `create_by` BIGINT NULL DEFAULT NULL COMMENT '创建者',
    `create_time` DATETIME NULL DEFAULT NULL COMMENT '创建时间',
    `update_by` BIGINT NULL DEFAULT NULL COMMENT '更新者',
    `update_time` DATETIME NULL DEFAULT NULL COMMENT '更新时间',
    `del_flag` CHAR(1) NULL DEFAULT '0' COMMENT '删除标志（0正常 1删除）',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='项目图纸工作量项表';

CALL ensure_column('doc_project_drawing_work_item', 'tenant_id',
    'ALTER TABLE `doc_project_drawing_work_item` ADD COLUMN `tenant_id` VARCHAR(20) NOT NULL DEFAULT ''000000'' COMMENT ''租户编号'' AFTER `project_id`');
CALL ensure_column('doc_project_drawing_work_item', 'work_item_name',
    'ALTER TABLE `doc_project_drawing_work_item` ADD COLUMN `work_item_name` VARCHAR(255) NULL DEFAULT NULL COMMENT ''工作量名称'' AFTER `drawing_id`');
CALL ensure_column('doc_project_drawing_work_item', 'technician',
    'ALTER TABLE `doc_project_drawing_work_item` ADD COLUMN `technician` DECIMAL(20,5) NOT NULL DEFAULT 0 COMMENT ''技工'' AFTER `work_item_name`');
CALL ensure_column('doc_project_drawing_work_item', 'technician_coefficient',
    'ALTER TABLE `doc_project_drawing_work_item` ADD COLUMN `technician_coefficient` DECIMAL(20,5) NOT NULL DEFAULT 1 COMMENT ''技工系数'' AFTER `technician`');
CALL ensure_column('doc_project_drawing_work_item', 'general_worker',
    'ALTER TABLE `doc_project_drawing_work_item` ADD COLUMN `general_worker` DECIMAL(20,5) NOT NULL DEFAULT 0 COMMENT ''普工'' AFTER `technician_coefficient`');
CALL ensure_column('doc_project_drawing_work_item', 'general_worker_coefficient',
    'ALTER TABLE `doc_project_drawing_work_item` ADD COLUMN `general_worker_coefficient` DECIMAL(20,5) NOT NULL DEFAULT 1 COMMENT ''普工系数'' AFTER `general_worker`');
CALL ensure_column('doc_project_drawing_work_item', 'machine_shift',
    'ALTER TABLE `doc_project_drawing_work_item` ADD COLUMN `machine_shift` DECIMAL(20,5) NOT NULL DEFAULT 0 COMMENT ''机械台班'' AFTER `general_worker_coefficient`');
CALL ensure_column('doc_project_drawing_work_item', 'machine_shift_unit_price',
    'ALTER TABLE `doc_project_drawing_work_item` ADD COLUMN `machine_shift_unit_price` DECIMAL(20,5) NOT NULL DEFAULT 0 COMMENT ''机械台班单价'' AFTER `machine_shift`');
CALL ensure_column('doc_project_drawing_work_item', 'machine_shift_coefficient',
    'ALTER TABLE `doc_project_drawing_work_item` ADD COLUMN `machine_shift_coefficient` DECIMAL(20,5) NOT NULL DEFAULT 1 COMMENT ''机械台班系数'' AFTER `machine_shift_unit_price`');
CALL ensure_column('doc_project_drawing_work_item', 'instrument_shift',
    'ALTER TABLE `doc_project_drawing_work_item` ADD COLUMN `instrument_shift` DECIMAL(20,5) NOT NULL DEFAULT 0 COMMENT ''仪器仪表台班'' AFTER `machine_shift_coefficient`');
CALL ensure_column('doc_project_drawing_work_item', 'instrument_shift_unit_price',
    'ALTER TABLE `doc_project_drawing_work_item` ADD COLUMN `instrument_shift_unit_price` DECIMAL(20,5) NOT NULL DEFAULT 0 COMMENT ''仪器仪表台班单价'' AFTER `instrument_shift`');
CALL ensure_column('doc_project_drawing_work_item', 'instrument_shift_coefficient',
    'ALTER TABLE `doc_project_drawing_work_item` ADD COLUMN `instrument_shift_coefficient` DECIMAL(20,5) NOT NULL DEFAULT 1 COMMENT ''仪器仪表系数'' AFTER `instrument_shift_unit_price`');
CALL ensure_column('doc_project_drawing_work_item', 'material_quantity',
    'ALTER TABLE `doc_project_drawing_work_item` ADD COLUMN `material_quantity` DECIMAL(20,5) NOT NULL DEFAULT 0 COMMENT ''材料数量'' AFTER `instrument_shift_coefficient`');
CALL ensure_column('doc_project_drawing_work_item', 'material_unit_price',
    'ALTER TABLE `doc_project_drawing_work_item` ADD COLUMN `material_unit_price` DECIMAL(20,5) NOT NULL DEFAULT 0 COMMENT ''材料单价'' AFTER `material_quantity`');

-- Legacy columns are no longer written by the application. Keep them for history,
-- but relax constraints so existing deployments can accept the new payload shape.
CALL relax_legacy_column('doc_project_drawing_work_item', 'work_item_code',
    'ALTER TABLE `doc_project_drawing_work_item` MODIFY COLUMN `work_item_code` VARCHAR(64) NULL DEFAULT NULL COMMENT ''工作量编码''');
CALL relax_legacy_column('doc_project_drawing_work_item', 'category',
    'ALTER TABLE `doc_project_drawing_work_item` MODIFY COLUMN `category` VARCHAR(64) NULL DEFAULT NULL COMMENT ''分类''');
CALL relax_legacy_column('doc_project_drawing_work_item', 'unit',
    'ALTER TABLE `doc_project_drawing_work_item` MODIFY COLUMN `unit` VARCHAR(32) NULL DEFAULT NULL COMMENT ''单位''');
CALL relax_legacy_column('doc_project_drawing_work_item', 'quantity',
    'ALTER TABLE `doc_project_drawing_work_item` MODIFY COLUMN `quantity` DECIMAL(20,5) NULL DEFAULT 0 COMMENT ''数量''');
CALL relax_legacy_column('doc_project_drawing_work_item', 'include_in_estimate',
    'ALTER TABLE `doc_project_drawing_work_item` MODIFY COLUMN `include_in_estimate` TINYINT(1) NULL DEFAULT 1 COMMENT ''计入估算''');
CALL relax_legacy_column('doc_project_drawing_work_item', 'remark',
    'ALTER TABLE `doc_project_drawing_work_item` MODIFY COLUMN `remark` VARCHAR(500) NULL DEFAULT NULL COMMENT ''备注''');

CALL ensure_index('doc_project_drawing_work_item', 'idx_drawing_work_item_project',
    'ALTER TABLE `doc_project_drawing_work_item` ADD INDEX `idx_drawing_work_item_project` (`project_id`)');
CALL ensure_index('doc_project_drawing_work_item', 'idx_drawing_work_item_drawing',
    'ALTER TABLE `doc_project_drawing_work_item` ADD INDEX `idx_drawing_work_item_drawing` (`drawing_id`)');

DROP PROCEDURE IF EXISTS ensure_column;
DROP PROCEDURE IF EXISTS ensure_index;
DROP PROCEDURE IF EXISTS relax_legacy_column;

SET FOREIGN_KEY_CHECKS = 1;
