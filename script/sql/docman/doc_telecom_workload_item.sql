/*
 Navicat Premium Data Transfer
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS `doc_telecom_workload_item`;
CREATE TABLE `doc_telecom_workload_item` (
  `id` bigint NOT NULL COMMENT '工作量基础项ID',
  `item_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '工作量编码',
  `item_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '工作量名称',
  `tenant_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '000000' COMMENT '租户编号',
  `category` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '分类',
  `unit` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '单位',
  `default_price` decimal(20,5) NULL DEFAULT NULL COMMENT '默认单价',
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
  `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '说明',
  `sort_order` int NULL DEFAULT 0 COMMENT '排序',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'active' COMMENT '状态',
  `create_dept` bigint NULL DEFAULT NULL COMMENT '创建部门',
  `create_by` bigint NULL DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint NULL DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '0' COMMENT '删除标志（0正常 1删除）',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_workload_item_name`(`item_name`) USING BTREE,
  INDEX `idx_workload_item_status_sort`(`status`, `sort_order`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '电信工作量基础维护表' ROW_FORMAT = Dynamic;

SET FOREIGN_KEY_CHECKS = 1;
