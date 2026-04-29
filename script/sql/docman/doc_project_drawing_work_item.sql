/*
 Navicat Premium Data Transfer
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS `doc_project_drawing_work_item`;
CREATE TABLE `doc_project_drawing_work_item` (
  `id` bigint NOT NULL COMMENT '图纸工作量ID',
  `project_id` bigint NOT NULL COMMENT '项目ID',
  `tenant_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '000000' COMMENT '租户编号',
  `drawing_id` bigint NOT NULL COMMENT '图纸ID',
  `work_item_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '工作量名称',
  `technician` decimal(20,5) NOT NULL DEFAULT 0 COMMENT '技工',
  `technician_coefficient` decimal(20,5) NOT NULL DEFAULT 1 COMMENT '技工系数',
  `general_worker` decimal(20,5) NOT NULL DEFAULT 0 COMMENT '普工',
  `general_worker_coefficient` decimal(20,5) NOT NULL DEFAULT 1 COMMENT '普工系数',
  `machine_shift` decimal(20,5) NOT NULL DEFAULT 0 COMMENT '机械台班',
  `machine_shift_unit_price` decimal(20,5) NOT NULL DEFAULT 0 COMMENT '机械台班单价',
  `machine_shift_coefficient` decimal(20,5) NOT NULL DEFAULT 1 COMMENT '机械台班系数',
  `instrument_shift` decimal(20,5) NOT NULL DEFAULT 0 COMMENT '仪器仪表台班',
  `instrument_shift_unit_price` decimal(20,5) NOT NULL DEFAULT 0 COMMENT '仪器仪表台班单价',
  `instrument_shift_coefficient` decimal(20,5) NOT NULL DEFAULT 1 COMMENT '仪器仪表系数',
  `material_quantity` decimal(20,5) NOT NULL DEFAULT 0 COMMENT '材料数量',
  `material_unit_price` decimal(20,5) NOT NULL DEFAULT 0 COMMENT '材料单价',
  `create_dept` bigint NULL DEFAULT NULL COMMENT '创建部门',
  `create_by` bigint NULL DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint NULL DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '0' COMMENT '删除标志（0正常 1删除）',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_drawing_work_item_project`(`project_id`) USING BTREE,
  INDEX `idx_drawing_work_item_drawing`(`drawing_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '项目图纸工作量项表' ROW_FORMAT = Dynamic;

SET FOREIGN_KEY_CHECKS = 1;
