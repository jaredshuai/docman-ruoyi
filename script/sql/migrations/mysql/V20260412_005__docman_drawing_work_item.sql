SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DROP PROCEDURE IF EXISTS ensure_index_if_table_exists;

DELIMITER $$

CREATE PROCEDURE ensure_index_if_table_exists(IN p_table VARCHAR(128), IN p_index VARCHAR(128), IN p_ddl TEXT)
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.tables
        WHERE table_schema = DATABASE()
          AND table_name = p_table
    ) AND NOT EXISTS (
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

CREATE TABLE IF NOT EXISTS `doc_project_drawing_work_item` (
    `id` BIGINT NOT NULL COMMENT '图纸工作量项ID',
    `project_id` BIGINT NOT NULL COMMENT '项目ID',
    `tenant_id` VARCHAR(20) NOT NULL DEFAULT '000000' COMMENT '租户编号',
    `drawing_id` BIGINT NOT NULL COMMENT '图纸ID',
    `work_item_code` VARCHAR(128) NULL DEFAULT NULL COMMENT '工作量编码',
    `work_item_name` VARCHAR(255) NULL DEFAULT NULL COMMENT '工作量名称',
    `category` VARCHAR(128) NULL DEFAULT NULL COMMENT '分类',
    `unit` VARCHAR(64) NULL DEFAULT NULL COMMENT '单位',
    `quantity` DECIMAL(20,5) NULL DEFAULT NULL COMMENT '数量',
    `include_in_estimate` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否计入估算',
    `remark` VARCHAR(500) NULL DEFAULT NULL COMMENT '备注',
    `create_dept` BIGINT NULL DEFAULT NULL COMMENT '创建部门',
    `create_by` BIGINT NULL DEFAULT NULL COMMENT '创建者',
    `create_time` DATETIME NULL DEFAULT NULL COMMENT '创建时间',
    `update_by` BIGINT NULL DEFAULT NULL COMMENT '更新者',
    `update_time` DATETIME NULL DEFAULT NULL COMMENT '更新时间',
    `del_flag` CHAR(1) NULL DEFAULT '0' COMMENT '删除标志（0正常 1删除）',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='项目图纸工作量项表';

CALL ensure_index_if_table_exists('doc_project_drawing_work_item', 'idx_drawing_work_item_project',
    'ALTER TABLE `doc_project_drawing_work_item` ADD INDEX `idx_drawing_work_item_project` (`project_id`)');

CALL ensure_index_if_table_exists('doc_project_drawing_work_item', 'idx_drawing_work_item_drawing',
    'ALTER TABLE `doc_project_drawing_work_item` ADD INDEX `idx_drawing_work_item_drawing` (`drawing_id`)');

DROP PROCEDURE IF EXISTS ensure_index_if_table_exists;

SET FOREIGN_KEY_CHECKS = 1;
