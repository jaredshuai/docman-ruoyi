/*
 Navicat Premium Data Transfer
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS `doc_project_estimate_snapshot`;
CREATE TABLE `doc_project_estimate_snapshot`  (
  `id` bigint NOT NULL COMMENT '估算快照ID',
  `project_id` bigint NOT NULL COMMENT '项目ID',
  `estimate_type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'initial_estimate' COMMENT '估算类型',
  `estimate_amount` decimal(18, 2) NOT NULL COMMENT '估算金额',
  `drawing_count` bigint NOT NULL DEFAULT 0 COMMENT '计入项目的图纸数量',
  `visa_count` bigint NOT NULL DEFAULT 0 COMMENT '计入项目的签证数量',
  `status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'mocked' COMMENT '估算状态',
  `summary` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '估算摘要',
  `create_dept` bigint NULL DEFAULT NULL COMMENT '创建部门',
  `create_by` bigint NULL DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint NULL DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '0' COMMENT '删除标志（0正常 1删除）',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_project_estimate_snapshot_project`(`project_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '项目估算结果快照表' ROW_FORMAT = Dynamic;

SET FOREIGN_KEY_CHECKS = 1;
