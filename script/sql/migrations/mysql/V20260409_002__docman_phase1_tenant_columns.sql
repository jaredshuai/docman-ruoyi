SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DROP PROCEDURE IF EXISTS ensure_column_if_table_exists;

DELIMITER $$

CREATE PROCEDURE ensure_column_if_table_exists(IN p_table VARCHAR(128), IN p_column VARCHAR(128), IN p_ddl TEXT)
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.tables
        WHERE table_schema = DATABASE()
          AND table_name = p_table
    ) AND NOT EXISTS (
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

CALL ensure_column_if_table_exists('doc_project_add_record', 'tenant_id',
    'ALTER TABLE `doc_project_add_record` ADD COLUMN `tenant_id` VARCHAR(20) NOT NULL DEFAULT ''000000'' COMMENT ''租户编号'' AFTER `project_id`');
CALL ensure_column_if_table_exists('doc_project_add_record_detail', 'tenant_id',
    'ALTER TABLE `doc_project_add_record_detail` ADD COLUMN `tenant_id` VARCHAR(20) NOT NULL DEFAULT ''000000'' COMMENT ''租户编号'' AFTER `project_add_record_id`');
CALL ensure_column_if_table_exists('doc_project_type', 'tenant_id',
    'ALTER TABLE `doc_project_type` ADD COLUMN `tenant_id` VARCHAR(20) NOT NULL DEFAULT ''000000'' COMMENT ''租户编号'' AFTER `customer_type`');
CALL ensure_column_if_table_exists('doc_workflow_template', 'tenant_id',
    'ALTER TABLE `doc_workflow_template` ADD COLUMN `tenant_id` VARCHAR(20) NOT NULL DEFAULT ''000000'' COMMENT ''租户编号'' AFTER `project_type_code`');
CALL ensure_column_if_table_exists('doc_workflow_template_node', 'tenant_id',
    'ALTER TABLE `doc_workflow_template_node` ADD COLUMN `tenant_id` VARCHAR(20) NOT NULL DEFAULT ''000000'' COMMENT ''租户编号'' AFTER `node_name`');
CALL ensure_column_if_table_exists('doc_workflow_node_task', 'tenant_id',
    'ALTER TABLE `doc_workflow_node_task` ADD COLUMN `tenant_id` VARCHAR(20) NOT NULL DEFAULT ''000000'' COMMENT ''租户编号'' AFTER `task_type`');
CALL ensure_column_if_table_exists('doc_project_drawing', 'tenant_id',
    'ALTER TABLE `doc_project_drawing` ADD COLUMN `tenant_id` VARCHAR(20) NOT NULL DEFAULT ''000000'' COMMENT ''租户编号'' AFTER `project_id`');
CALL ensure_column_if_table_exists('doc_project_visa', 'tenant_id',
    'ALTER TABLE `doc_project_visa` ADD COLUMN `tenant_id` VARCHAR(20) NOT NULL DEFAULT ''000000'' COMMENT ''租户编号'' AFTER `project_id`');
CALL ensure_column_if_table_exists('doc_project_order', 'tenant_id',
    'ALTER TABLE `doc_project_order` ADD COLUMN `tenant_id` VARCHAR(20) NOT NULL DEFAULT ''000000'' COMMENT ''租户编号'' AFTER `project_id`');
CALL ensure_column_if_table_exists('doc_project_runtime', 'tenant_id',
    'ALTER TABLE `doc_project_runtime` ADD COLUMN `tenant_id` VARCHAR(20) NOT NULL DEFAULT ''000000'' COMMENT ''租户编号'' AFTER `workflow_template_id`');
CALL ensure_column_if_table_exists('doc_project_node_task_runtime', 'tenant_id',
    'ALTER TABLE `doc_project_node_task_runtime` ADD COLUMN `tenant_id` VARCHAR(20) NOT NULL DEFAULT ''000000'' COMMENT ''租户编号'' AFTER `task_code`');
CALL ensure_column_if_table_exists('doc_project_estimate_snapshot', 'tenant_id',
    'ALTER TABLE `doc_project_estimate_snapshot` ADD COLUMN `tenant_id` VARCHAR(20) NOT NULL DEFAULT ''000000'' COMMENT ''租户编号'' AFTER `project_id`');
CALL ensure_column_if_table_exists('doc_project_balance_adjustment', 'tenant_id',
    'ALTER TABLE `doc_project_balance_adjustment` ADD COLUMN `tenant_id` VARCHAR(20) NOT NULL DEFAULT ''000000'' COMMENT ''租户编号'' AFTER `project_id`');

DROP PROCEDURE IF EXISTS ensure_column_if_table_exists;

SET FOREIGN_KEY_CHECKS = 1;
