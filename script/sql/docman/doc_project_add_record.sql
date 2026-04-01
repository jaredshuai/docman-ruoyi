/*
 Navicat Premium Data Transfer

 Source Server         : 10.34.200.174_3306
 Source Server Type    : MySQL
 Source Server Version : 80045 (8.0.45-0ubuntu0.22.04.1)
 Source Host           : 10.34.200.174:3306
 Source Schema         : app_db

 Target Server Type    : MySQL
 Target Server Version : 80045 (8.0.45-0ubuntu0.22.04.1)
 File Encoding         : 65001

 Date: 01/04/2026 15:43:19
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for doc_project_add_record
-- ----------------------------
DROP TABLE IF EXISTS `doc_project_add_record`;
CREATE TABLE `doc_project_add_record`  (
  `id` bigint NOT NULL COMMENT '增加工作量记录id',
  `project_id` bigint NOT NULL COMMENT '项目id',
  `enable` tinyint NULL DEFAULT NULL COMMENT '是否启用',
  `estimated_price` decimal(20, 5) NULL DEFAULT NULL COMMENT '预估价格',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '备注',
  `create_dept` bigint NULL DEFAULT NULL COMMENT '创建部门',
  `create_by` bigint NULL DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint NULL DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '0' COMMENT '删除标志（0正常 1删除）',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '项目工作量记录表' ROW_FORMAT = Dynamic;

SET FOREIGN_KEY_CHECKS = 1;
