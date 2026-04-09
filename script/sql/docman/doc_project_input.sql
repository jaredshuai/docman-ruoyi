/*
 Navicat Premium Data Transfer
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS `doc_project_drawing`;
CREATE TABLE `doc_project_drawing`  (
  `id` bigint NOT NULL COMMENT '图纸记录ID',
  `project_id` bigint NOT NULL COMMENT '项目ID',
  `drawing_code` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '图纸编码/图号',
  `order_serial_no` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '订单流水号',
  `work_content` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '工作内容',
  `include_in_project` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否计入当前项目',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '备注',
  `create_dept` bigint NULL DEFAULT NULL COMMENT '创建部门',
  `create_by` bigint NULL DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint NULL DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '0' COMMENT '删除标志（0正常 1删除）',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_project_drawing_project`(`project_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '项目图纸录入表' ROW_FORMAT = Dynamic;

DROP TABLE IF EXISTS `doc_project_visa`;
CREATE TABLE `doc_project_visa`  (
  `id` bigint NOT NULL COMMENT '签证记录ID',
  `project_id` bigint NOT NULL COMMENT '项目ID',
  `reason` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '签证原因',
  `content_basis` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '签证内容及依据',
  `amount` decimal(12, 2) NULL DEFAULT NULL COMMENT '签证金额',
  `visa_date` datetime NULL DEFAULT NULL COMMENT '签证日期',
  `include_in_project` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否计入当前项目',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '备注',
  `create_dept` bigint NULL DEFAULT NULL COMMENT '创建部门',
  `create_by` bigint NULL DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint NULL DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '0' COMMENT '删除标志（0正常 1删除）',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_project_visa_project`(`project_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '项目签证录入表' ROW_FORMAT = Dynamic;

SET FOREIGN_KEY_CHECKS = 1;
