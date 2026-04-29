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

CREATE TABLE IF NOT EXISTS `doc_telecom_workload_item` (
    `id` BIGINT NOT NULL COMMENT '工作量基础项ID',
    `item_code` VARCHAR(64) NULL DEFAULT NULL COMMENT '工作量编码',
    `item_name` VARCHAR(255) NOT NULL COMMENT '工作量名称',
    `tenant_id` VARCHAR(20) NOT NULL DEFAULT '000000' COMMENT '租户编号',
    `category` VARCHAR(64) NULL DEFAULT NULL COMMENT '分类',
    `unit` VARCHAR(32) NULL DEFAULT NULL COMMENT '单位',
    `default_price` DECIMAL(20,5) NULL DEFAULT NULL COMMENT '默认单价',
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
    `description` VARCHAR(500) NULL DEFAULT NULL COMMENT '说明',
    `sort_order` INT NULL DEFAULT 0 COMMENT '排序',
    `status` VARCHAR(20) NOT NULL DEFAULT 'active' COMMENT '状态',
    `create_dept` BIGINT NULL DEFAULT NULL COMMENT '创建部门',
    `create_by` BIGINT NULL DEFAULT NULL COMMENT '创建者',
    `create_time` DATETIME NULL DEFAULT NULL COMMENT '创建时间',
    `update_by` BIGINT NULL DEFAULT NULL COMMENT '更新者',
    `update_time` DATETIME NULL DEFAULT NULL COMMENT '更新时间',
    `del_flag` CHAR(1) NULL DEFAULT '0' COMMENT '删除标志（0正常 1删除）',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='电信工作量基础维护表';

CALL ensure_column('doc_telecom_workload_item', 'item_code',
    'ALTER TABLE `doc_telecom_workload_item` ADD COLUMN `item_code` VARCHAR(64) NULL DEFAULT NULL COMMENT ''工作量编码'' AFTER `id`');
CALL ensure_column('doc_telecom_workload_item', 'item_name',
    'ALTER TABLE `doc_telecom_workload_item` ADD COLUMN `item_name` VARCHAR(255) NULL DEFAULT NULL COMMENT ''工作量名称'' AFTER `item_code`');
CALL ensure_column('doc_telecom_workload_item', 'tenant_id',
    'ALTER TABLE `doc_telecom_workload_item` ADD COLUMN `tenant_id` VARCHAR(20) NOT NULL DEFAULT ''000000'' COMMENT ''租户编号'' AFTER `item_name`');
CALL ensure_column('doc_telecom_workload_item', 'category',
    'ALTER TABLE `doc_telecom_workload_item` ADD COLUMN `category` VARCHAR(64) NULL DEFAULT NULL COMMENT ''分类'' AFTER `tenant_id`');
CALL ensure_column('doc_telecom_workload_item', 'unit',
    'ALTER TABLE `doc_telecom_workload_item` ADD COLUMN `unit` VARCHAR(32) NULL DEFAULT NULL COMMENT ''单位'' AFTER `category`');
CALL ensure_column('doc_telecom_workload_item', 'default_price',
    'ALTER TABLE `doc_telecom_workload_item` ADD COLUMN `default_price` DECIMAL(20,5) NULL DEFAULT NULL COMMENT ''默认单价'' AFTER `unit`');
CALL ensure_column('doc_telecom_workload_item', 'technician',
    'ALTER TABLE `doc_telecom_workload_item` ADD COLUMN `technician` DECIMAL(20,5) NOT NULL DEFAULT 0 COMMENT ''技工'' AFTER `default_price`');
CALL ensure_column('doc_telecom_workload_item', 'technician_coefficient',
    'ALTER TABLE `doc_telecom_workload_item` ADD COLUMN `technician_coefficient` DECIMAL(20,5) NOT NULL DEFAULT 1 COMMENT ''技工系数'' AFTER `technician`');
CALL ensure_column('doc_telecom_workload_item', 'general_worker',
    'ALTER TABLE `doc_telecom_workload_item` ADD COLUMN `general_worker` DECIMAL(20,5) NOT NULL DEFAULT 0 COMMENT ''普工'' AFTER `technician_coefficient`');
CALL ensure_column('doc_telecom_workload_item', 'general_worker_coefficient',
    'ALTER TABLE `doc_telecom_workload_item` ADD COLUMN `general_worker_coefficient` DECIMAL(20,5) NOT NULL DEFAULT 1 COMMENT ''普工系数'' AFTER `general_worker`');
CALL ensure_column('doc_telecom_workload_item', 'machine_shift',
    'ALTER TABLE `doc_telecom_workload_item` ADD COLUMN `machine_shift` DECIMAL(20,5) NOT NULL DEFAULT 0 COMMENT ''机械台班'' AFTER `general_worker_coefficient`');
CALL ensure_column('doc_telecom_workload_item', 'machine_shift_unit_price',
    'ALTER TABLE `doc_telecom_workload_item` ADD COLUMN `machine_shift_unit_price` DECIMAL(20,5) NOT NULL DEFAULT 0 COMMENT ''机械台班单价'' AFTER `machine_shift`');
CALL ensure_column('doc_telecom_workload_item', 'machine_shift_coefficient',
    'ALTER TABLE `doc_telecom_workload_item` ADD COLUMN `machine_shift_coefficient` DECIMAL(20,5) NOT NULL DEFAULT 1 COMMENT ''机械台班系数'' AFTER `machine_shift_unit_price`');
CALL ensure_column('doc_telecom_workload_item', 'instrument_shift',
    'ALTER TABLE `doc_telecom_workload_item` ADD COLUMN `instrument_shift` DECIMAL(20,5) NOT NULL DEFAULT 0 COMMENT ''仪器仪表台班'' AFTER `machine_shift_coefficient`');
CALL ensure_column('doc_telecom_workload_item', 'instrument_shift_unit_price',
    'ALTER TABLE `doc_telecom_workload_item` ADD COLUMN `instrument_shift_unit_price` DECIMAL(20,5) NOT NULL DEFAULT 0 COMMENT ''仪器仪表台班单价'' AFTER `instrument_shift`');
CALL ensure_column('doc_telecom_workload_item', 'instrument_shift_coefficient',
    'ALTER TABLE `doc_telecom_workload_item` ADD COLUMN `instrument_shift_coefficient` DECIMAL(20,5) NOT NULL DEFAULT 1 COMMENT ''仪器仪表系数'' AFTER `instrument_shift_unit_price`');
CALL ensure_column('doc_telecom_workload_item', 'material_quantity',
    'ALTER TABLE `doc_telecom_workload_item` ADD COLUMN `material_quantity` DECIMAL(20,5) NOT NULL DEFAULT 0 COMMENT ''材料数量'' AFTER `instrument_shift_coefficient`');
CALL ensure_column('doc_telecom_workload_item', 'material_unit_price',
    'ALTER TABLE `doc_telecom_workload_item` ADD COLUMN `material_unit_price` DECIMAL(20,5) NOT NULL DEFAULT 0 COMMENT ''材料单价'' AFTER `material_quantity`');
CALL ensure_column('doc_telecom_workload_item', 'description',
    'ALTER TABLE `doc_telecom_workload_item` ADD COLUMN `description` VARCHAR(500) NULL DEFAULT NULL COMMENT ''说明'' AFTER `material_unit_price`');
CALL ensure_column('doc_telecom_workload_item', 'sort_order',
    'ALTER TABLE `doc_telecom_workload_item` ADD COLUMN `sort_order` INT NULL DEFAULT 0 COMMENT ''排序'' AFTER `description`');
CALL ensure_column('doc_telecom_workload_item', 'status',
    'ALTER TABLE `doc_telecom_workload_item` ADD COLUMN `status` VARCHAR(20) NOT NULL DEFAULT ''active'' COMMENT ''状态'' AFTER `sort_order`');
CALL ensure_column('doc_telecom_workload_item', 'create_dept',
    'ALTER TABLE `doc_telecom_workload_item` ADD COLUMN `create_dept` BIGINT NULL DEFAULT NULL COMMENT ''创建部门'' AFTER `status`');
CALL ensure_column('doc_telecom_workload_item', 'create_by',
    'ALTER TABLE `doc_telecom_workload_item` ADD COLUMN `create_by` BIGINT NULL DEFAULT NULL COMMENT ''创建者'' AFTER `create_dept`');
CALL ensure_column('doc_telecom_workload_item', 'create_time',
    'ALTER TABLE `doc_telecom_workload_item` ADD COLUMN `create_time` DATETIME NULL DEFAULT NULL COMMENT ''创建时间'' AFTER `create_by`');
CALL ensure_column('doc_telecom_workload_item', 'update_by',
    'ALTER TABLE `doc_telecom_workload_item` ADD COLUMN `update_by` BIGINT NULL DEFAULT NULL COMMENT ''更新者'' AFTER `create_time`');
CALL ensure_column('doc_telecom_workload_item', 'update_time',
    'ALTER TABLE `doc_telecom_workload_item` ADD COLUMN `update_time` DATETIME NULL DEFAULT NULL COMMENT ''更新时间'' AFTER `update_by`');
CALL ensure_column('doc_telecom_workload_item', 'del_flag',
    'ALTER TABLE `doc_telecom_workload_item` ADD COLUMN `del_flag` CHAR(1) NULL DEFAULT ''0'' COMMENT ''删除标志（0正常 1删除）'' AFTER `update_time`');

CALL ensure_index('doc_telecom_workload_item', 'idx_workload_item_name',
    'ALTER TABLE `doc_telecom_workload_item` ADD INDEX `idx_workload_item_name` (`item_name`)');
CALL ensure_index('doc_telecom_workload_item', 'idx_workload_item_status_sort',
    'ALTER TABLE `doc_telecom_workload_item` ADD INDEX `idx_workload_item_status_sort` (`status`, `sort_order`)');

INSERT INTO `sys_menu` (
    `menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`,
    `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`,
    `create_dept`, `create_by`, `create_time`, `remark`
)
SELECT 3060, '工作量基础维护', 3000, 7, 'workload-item', 'docman/workloadItem/index', '',
       1, 0, 'C', '0', '0', 'docman:workload-item:list', 'table',
       103, 1, NOW(), '工作量基础维护菜单'
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 3060);

INSERT INTO `sys_menu` (
    `menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`,
    `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`,
    `create_dept`, `create_by`, `create_time`, `remark`
)
SELECT 3061, '工作量查询', 3060, 1, '', '', '',
       1, 0, 'F', '0', '0', 'docman:workload-item:query', '#',
       103, 1, NOW(), ''
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 3061);

INSERT INTO `sys_menu` (
    `menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`,
    `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`,
    `create_dept`, `create_by`, `create_time`, `remark`
)
SELECT 3062, '工作量新增', 3060, 2, '', '', '',
       1, 0, 'F', '0', '0', 'docman:workload-item:edit', '#',
       103, 1, NOW(), ''
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 3062);

INSERT INTO `sys_menu` (
    `menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`,
    `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`,
    `create_dept`, `create_by`, `create_time`, `remark`
)
SELECT 3063, '工作量修改', 3060, 3, '', '', '',
       1, 0, 'F', '0', '0', 'docman:workload-item:edit', '#',
       103, 1, NOW(), ''
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 3063);

INSERT INTO `sys_menu` (
    `menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`,
    `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`,
    `create_dept`, `create_by`, `create_time`, `remark`
)
SELECT 3064, '工作量删除', 3060, 4, '', '', '',
       1, 0, 'F', '0', '0', 'docman:workload-item:remove', '#',
       103, 1, NOW(), ''
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 3064);

DROP PROCEDURE IF EXISTS ensure_column;
DROP PROCEDURE IF EXISTS ensure_index;

SET FOREIGN_KEY_CHECKS = 1;
