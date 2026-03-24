-- ==========================================
-- 文档管理模块 建表SQL
-- ==========================================

-- 项目表
CREATE TABLE doc_project (
    id              BIGINT       NOT NULL COMMENT '项目ID',
    name            VARCHAR(200) NOT NULL COMMENT '项目名称',
    customer_type   VARCHAR(20)  NOT NULL COMMENT '客户类型（telecom/social）',
    business_type   VARCHAR(20)  NOT NULL COMMENT '业务类型（pipeline/weak_current）',
    document_category VARCHAR(20) NOT NULL COMMENT '文档类别（telecom/internal/customer）',
    status          VARCHAR(20)  NOT NULL DEFAULT 'active' COMMENT '项目状态（active/archived）',
    owner_id        BIGINT       NOT NULL COMMENT '负责人ID',
    nas_base_path   VARCHAR(500) COMMENT '群晖NAS基础路径',
    nas_dir_status  VARCHAR(20)  NOT NULL DEFAULT 'pending' COMMENT 'NAS目录状态（pending/created/failed）',
    remark          VARCHAR(500) COMMENT '备注',
    create_dept     BIGINT       COMMENT '创建部门',
    create_by       BIGINT       COMMENT '创建者',
    create_time     DATETIME     COMMENT '创建时间',
    update_by       BIGINT       COMMENT '更新者',
    update_time     DATETIME     COMMENT '更新时间',
    del_flag        CHAR(1)      DEFAULT '0' COMMENT '删除标志（0正常 1删除）',
    PRIMARY KEY (id)
) ENGINE=InnoDB COMMENT='项目表';

-- 项目成员关联表
CREATE TABLE doc_project_member (
    id              BIGINT       NOT NULL COMMENT '主键',
    project_id      BIGINT       NOT NULL COMMENT '项目ID',
    user_id         BIGINT       NOT NULL COMMENT '用户ID',
    role_type       VARCHAR(20)  NOT NULL DEFAULT 'viewer' COMMENT '角色类型（owner/editor/viewer）',
    create_time     DATETIME     COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_project_user (project_id, user_id)
) ENGINE=InnoDB COMMENT='项目成员关联表';

-- 文档记录表
CREATE TABLE doc_document_record (
    id              BIGINT       NOT NULL COMMENT '文档ID',
    project_id      BIGINT       NOT NULL COMMENT '所属项目',
    node_instance_id BIGINT      COMMENT '所属节点实例',
    plugin_id       VARCHAR(100) COMMENT '生成来源插件ID',
    source_type     VARCHAR(20)  NOT NULL COMMENT '来源类型（plugin/upload）',
    file_name       VARCHAR(300) NOT NULL COMMENT '中文可读文件名',
    nas_path        VARCHAR(500) NOT NULL COMMENT '群晖完整路径',
    oss_id          BIGINT       COMMENT '关联 sys_oss 表ID',
    status          VARCHAR(20)  NOT NULL DEFAULT 'pending' COMMENT '状态（pending/running/generated/failed/archived/obsolete）',
    generated_at    DATETIME     COMMENT '生成时间',
    archived_at     DATETIME     COMMENT '归档时间',
    create_dept     BIGINT       COMMENT '创建部门',
    create_by       BIGINT       COMMENT '创建者',
    create_time     DATETIME     COMMENT '创建时间',
    update_by       BIGINT       COMMENT '更新者',
    update_time     DATETIME     COMMENT '更新时间',
    del_flag        CHAR(1)      DEFAULT '0' COMMENT '删除标志',
    PRIMARY KEY (id),
    KEY idx_project (project_id),
    KEY idx_node_instance (node_instance_id)
) ENGINE=InnoDB COMMENT='文档记录表';

-- 节点上下文表
CREATE TABLE doc_node_context (
    id                  BIGINT       NOT NULL COMMENT '主键',
    process_instance_id BIGINT       NOT NULL COMMENT 'Warm-Flow流程实例ID',
    node_code           VARCHAR(100) NOT NULL COMMENT '节点编码',
    project_id          BIGINT       NOT NULL COMMENT '所属项目ID',
    process_variables   JSON         COMMENT '流程级变量',
    node_variables      JSON         COMMENT '节点级变量',
    document_facts      JSON         COMMENT '可复用文档事实',
    unstructured_content JSON        COMMENT '非结构化内容',
    create_time         DATETIME     COMMENT '创建时间',
    update_time         DATETIME     COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_instance_node (process_instance_id, node_code),
    KEY idx_project (project_id)
) ENGINE=InnoDB COMMENT='节点上下文表';

-- 流程-项目绑定配置表
CREATE TABLE doc_process_config (
    id                  BIGINT       NOT NULL COMMENT '主键',
    project_id          BIGINT       NOT NULL COMMENT '项目ID',
    definition_id       BIGINT       NOT NULL COMMENT 'Warm-Flow流程定义ID',
    instance_id         BIGINT       COMMENT 'Warm-Flow流程实例ID',
    status              VARCHAR(20)  NOT NULL DEFAULT 'pending' COMMENT '状态（pending/running/completed）',
    create_dept         BIGINT       COMMENT '创建部门',
    create_by           BIGINT       COMMENT '创建者',
    create_time         DATETIME     COMMENT '创建时间',
    update_by           BIGINT       COMMENT '更新者',
    update_time         DATETIME     COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_project (project_id)
) ENGINE=InnoDB COMMENT='流程-项目绑定配置表';

-- 归档包表
CREATE TABLE doc_archive_package (
    id              BIGINT       NOT NULL COMMENT '主键',
    project_id      BIGINT       NOT NULL COMMENT '项目ID',
    archive_no      VARCHAR(64)  NOT NULL COMMENT '归档编号',
    archive_version BIGINT       NOT NULL DEFAULT 1 COMMENT '归档版本',
    nas_archive_path VARCHAR(500) NOT NULL COMMENT 'NAS归档根路径',
    manifest        JSON         COMMENT '归档清单',
    snapshot_checksum VARCHAR(128) COMMENT '快照校验和',
    requested_at    DATETIME     COMMENT '归档申请时间',
    completed_at    DATETIME     COMMENT '归档完成时间',
    status          VARCHAR(20)  NOT NULL DEFAULT 'requested' COMMENT '状态（requested/generating/completed/failed）',
    create_dept     BIGINT       COMMENT '创建部门',
    create_by       BIGINT       COMMENT '创建者',
    create_time     DATETIME     COMMENT '创建时间',
    update_by       BIGINT       COMMENT '更新者',
    update_time     DATETIME     COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_archive_no (archive_no),
    KEY idx_project_version (project_id, archive_version)
) ENGINE=InnoDB COMMENT='归档包表';

-- 插件执行日志表
CREATE TABLE doc_plugin_execution_log (
    id                BIGINT       NOT NULL COMMENT '主键',
    project_id        BIGINT       NOT NULL COMMENT '项目ID',
    process_instance_id BIGINT     COMMENT '流程实例ID',
    node_code         VARCHAR(100) COMMENT '节点编码',
    plugin_id         VARCHAR(100) NOT NULL COMMENT '插件ID',
    plugin_name       VARCHAR(200) NOT NULL COMMENT '插件名称',
    status            VARCHAR(20)  NOT NULL COMMENT '执行状态（success/failed）',
    cost_ms           BIGINT       NOT NULL DEFAULT 0 COMMENT '耗时（毫秒）',
    generated_file_count INT       NOT NULL DEFAULT 0 COMMENT '生成文件数',
    error_message     VARCHAR(1000) COMMENT '错误信息',
    request_snapshot  LONGTEXT     COMMENT '请求快照',
    result_snapshot   LONGTEXT     COMMENT '结果快照',
    create_dept       BIGINT       COMMENT '创建部门',
    create_by         BIGINT       COMMENT '创建者',
    create_time       DATETIME     COMMENT '创建时间',
    update_by         BIGINT       COMMENT '更新者',
    update_time       DATETIME     COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_project_time (project_id, create_time),
    KEY idx_instance_node (process_instance_id, node_code),
    KEY idx_plugin (plugin_id)
) ENGINE=InnoDB COMMENT='插件执行日志表';

-- ==========================================
-- 菜单与权限
-- ==========================================

-- 一级菜单：文档管理
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, menu_type, perms, icon, create_by, create_time)
VALUES (3000, '文档管理', 0, 5, 'docman', NULL, 'M', '', 'documentation', 1, NOW());

-- 项目管理
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, menu_type, perms, icon, create_by, create_time)
VALUES (3001, '项目管理', 3000, 1, 'project', 'docman/project/index', 'C', 'docman:project:list', 'tree', 1, NOW());
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, menu_type, perms, create_by, create_time)
VALUES (3002, '项目查询', 3001, 1, 'F', 'docman:project:query', 1, NOW());
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, menu_type, perms, create_by, create_time)
VALUES (3003, '项目新增', 3001, 2, 'F', 'docman:project:add', 1, NOW());
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, menu_type, perms, create_by, create_time)
VALUES (3004, '项目修改', 3001, 3, 'F', 'docman:project:edit', 1, NOW());
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, menu_type, perms, create_by, create_time)
VALUES (3005, '项目删除', 3001, 4, 'F', 'docman:project:remove', 1, NOW());

-- 文档中心
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, menu_type, perms, icon, create_by, create_time)
VALUES (3010, '文档中心', 3000, 2, 'document', 'docman/document/index', 'C', 'docman:document:list', 'list', 1, NOW());
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, menu_type, perms, create_by, create_time)
VALUES (3011, '文档上传', 3010, 1, 'F', 'docman:document:upload', 1, NOW());

-- 流程编排
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, menu_type, perms, icon, create_by, create_time)
VALUES (3020, '流程编排', 3000, 3, 'process', 'docman/process/index', 'C', 'docman:process:query', 'cascader', 1, NOW());
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, menu_type, perms, create_by, create_time)
VALUES (3021, '流程绑定', 3020, 1, 'F', 'docman:process:bind', 1, NOW());
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, menu_type, perms, create_by, create_time)
VALUES (3022, '流程启动', 3020, 2, 'F', 'docman:process:start', 1, NOW());

-- 归档管理
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, menu_type, perms, icon, create_by, create_time)
VALUES (3030, '归档管理', 3000, 4, 'archive', 'docman/archive/index', 'C', 'docman:archive:query', 'zip', 1, NOW());
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, menu_type, perms, create_by, create_time)
VALUES (3031, '执行归档', 3030, 1, 'F', 'docman:archive:execute', 1, NOW());

-- 插件管理
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, menu_type, perms, icon, create_by, create_time)
VALUES (3040, '插件列表', 3000, 5, 'plugin', 'docman/plugin/index', 'C', 'docman:plugin:list', 'component', 1, NOW());

-- 文档详情查询权限
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, menu_type, perms, create_by, create_time)
VALUES (3014, '文档详情', 3010, 4, 'F', 'docman:document:query', 1, NOW());

-- 文档下载、删除权限
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, menu_type, perms, create_by, create_time)
VALUES (3012, '文档下载', 3010, 2, 'F', 'docman:document:download', 1, NOW());
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, menu_type, perms, create_by, create_time)
VALUES (3013, '文档删除', 3010, 3, 'F', 'docman:document:delete', 1, NOW());

-- ==========================================
-- 字典类型与字典数据
-- ==========================================

-- 项目状态
INSERT INTO sys_dict_type (dict_id, tenant_id, dict_name, dict_type, create_dept, create_by, create_time, update_by, update_time, remark)
VALUES (16, '000000', '项目状态', 'doc_project_status', 103, 1, sysdate(), NULL, NULL, '项目状态列表');
INSERT INTO sys_dict_data (dict_code, tenant_id, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, create_dept, create_by, create_time, update_by, update_time, remark)
VALUES (60, '000000', 1, '进行中', 'active', 'doc_project_status', '', 'primary', 'Y', 103, 1, sysdate(), NULL, NULL, '进行中');
INSERT INTO sys_dict_data (dict_code, tenant_id, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, create_dept, create_by, create_time, update_by, update_time, remark)
VALUES (61, '000000', 2, '已归档', 'archived', 'doc_project_status', '', 'info', 'N', 103, 1, sysdate(), NULL, NULL, '已归档');

-- 客户类型
INSERT INTO sys_dict_type (dict_id, tenant_id, dict_name, dict_type, create_dept, create_by, create_time, update_by, update_time, remark)
VALUES (17, '000000', '客户类型', 'doc_customer_type', 103, 1, sysdate(), NULL, NULL, '客户类型列表');
INSERT INTO sys_dict_data (dict_code, tenant_id, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, create_dept, create_by, create_time, update_by, update_time, remark)
VALUES (62, '000000', 1, '电信客户', 'telecom', 'doc_customer_type', '', 'primary', 'Y', 103, 1, sysdate(), NULL, NULL, '电信客户');
INSERT INTO sys_dict_data (dict_code, tenant_id, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, create_dept, create_by, create_time, update_by, update_time, remark)
VALUES (63, '000000', 2, '社会客户', 'social', 'doc_customer_type', '', 'success', 'N', 103, 1, sysdate(), NULL, NULL, '社会客户');

-- 业务类型
INSERT INTO sys_dict_type (dict_id, tenant_id, dict_name, dict_type, create_dept, create_by, create_time, update_by, update_time, remark)
VALUES (18, '000000', '业务类型', 'doc_business_type', 103, 1, sysdate(), NULL, NULL, '业务类型列表');
INSERT INTO sys_dict_data (dict_code, tenant_id, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, create_dept, create_by, create_time, update_by, update_time, remark)
VALUES (64, '000000', 1, '管线', 'pipeline', 'doc_business_type', '', 'primary', 'Y', 103, 1, sysdate(), NULL, NULL, '管线');
INSERT INTO sys_dict_data (dict_code, tenant_id, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, create_dept, create_by, create_time, update_by, update_time, remark)
VALUES (65, '000000', 2, '弱电', 'weak_current', 'doc_business_type', '', 'warning', 'N', 103, 1, sysdate(), NULL, NULL, '弱电');

-- 文档状态
INSERT INTO sys_dict_type (dict_id, tenant_id, dict_name, dict_type, create_dept, create_by, create_time, update_by, update_time, remark)
VALUES (19, '000000', '文档状态', 'doc_document_status', 103, 1, sysdate(), NULL, NULL, '文档状态列表');
INSERT INTO sys_dict_data (dict_code, tenant_id, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, create_dept, create_by, create_time, update_by, update_time, remark)
VALUES (66, '000000', 1, '待生成', 'pending', 'doc_document_status', '', 'info', 'Y', 103, 1, sysdate(), NULL, NULL, '待生成');
INSERT INTO sys_dict_data (dict_code, tenant_id, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, create_dept, create_by, create_time, update_by, update_time, remark)
VALUES (67, '000000', 2, '生成中', 'running', 'doc_document_status', '', 'warning', 'N', 103, 1, sysdate(), NULL, NULL, '生成中');
INSERT INTO sys_dict_data (dict_code, tenant_id, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, create_dept, create_by, create_time, update_by, update_time, remark)
VALUES (68, '000000', 3, '已生成', 'generated', 'doc_document_status', '', 'success', 'N', 103, 1, sysdate(), NULL, NULL, '已生成');
INSERT INTO sys_dict_data (dict_code, tenant_id, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, create_dept, create_by, create_time, update_by, update_time, remark)
VALUES (69, '000000', 4, '生成失败', 'failed', 'doc_document_status', '', 'danger', 'N', 103, 1, sysdate(), NULL, NULL, '生成失败');
INSERT INTO sys_dict_data (dict_code, tenant_id, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, create_dept, create_by, create_time, update_by, update_time, remark)
VALUES (70, '000000', 5, '已归档', 'archived', 'doc_document_status', '', 'primary', 'N', 103, 1, sysdate(), NULL, NULL, '已归档');
INSERT INTO sys_dict_data (dict_code, tenant_id, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, create_dept, create_by, create_time, update_by, update_time, remark)
VALUES (71, '000000', 6, '已失效', 'obsolete', 'doc_document_status', '', 'default', 'N', 103, 1, sysdate(), NULL, NULL, '已失效');

-- 文档来源
INSERT INTO sys_dict_type (dict_id, tenant_id, dict_name, dict_type, create_dept, create_by, create_time, update_by, update_time, remark)
VALUES (20, '000000', '文档来源', 'doc_source_type', 103, 1, sysdate(), NULL, NULL, '文档来源列表');
INSERT INTO sys_dict_data (dict_code, tenant_id, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, create_dept, create_by, create_time, update_by, update_time, remark)
VALUES (72, '000000', 1, '插件自动生成', 'plugin', 'doc_source_type', '', 'primary', 'Y', 103, 1, sysdate(), NULL, NULL, '插件自动生成');
INSERT INTO sys_dict_data (dict_code, tenant_id, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, create_dept, create_by, create_time, update_by, update_time, remark)
VALUES (73, '000000', 2, '手动上传', 'upload', 'doc_source_type', '', 'success', 'N', 103, 1, sysdate(), NULL, NULL, '手动上传');
INSERT INTO sys_dict_data (dict_code, tenant_id, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, create_dept, create_by, create_time, update_by, update_time, remark)
VALUES (74, '000000', 3, '归档清单', 'archive_manifest', 'doc_source_type', '', 'info', 'N', 103, 1, sysdate(), NULL, NULL, '归档清单');

-- 归档状态
INSERT INTO sys_dict_type (dict_id, tenant_id, dict_name, dict_type, create_dept, create_by, create_time, update_by, update_time, remark)
VALUES (21, '000000', '归档状态', 'doc_archive_status', 103, 1, sysdate(), NULL, NULL, '归档状态列表');
INSERT INTO sys_dict_data (dict_code, tenant_id, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, create_dept, create_by, create_time, update_by, update_time, remark)
VALUES (75, '000000', 1, '已申请', 'requested', 'doc_archive_status', '', 'info', 'Y', 103, 1, sysdate(), NULL, NULL, '已申请');
INSERT INTO sys_dict_data (dict_code, tenant_id, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, create_dept, create_by, create_time, update_by, update_time, remark)
VALUES (76, '000000', 2, '归档中', 'generating', 'doc_archive_status', '', 'warning', 'N', 103, 1, sysdate(), NULL, NULL, '归档中');
INSERT INTO sys_dict_data (dict_code, tenant_id, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, create_dept, create_by, create_time, update_by, update_time, remark)
VALUES (77, '000000', 3, '已完成', 'completed', 'doc_archive_status', '', 'success', 'N', 103, 1, sysdate(), NULL, NULL, '已完成');
INSERT INTO sys_dict_data (dict_code, tenant_id, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, create_dept, create_by, create_time, update_by, update_time, remark)
VALUES (78, '000000', 4, '归档失败', 'failed', 'doc_archive_status', '', 'danger', 'N', 103, 1, sysdate(), NULL, NULL, '归档失败');

-- 流程状态
INSERT INTO sys_dict_type (dict_id, tenant_id, dict_name, dict_type, create_dept, create_by, create_time, update_by, update_time, remark)
VALUES (22, '000000', '流程状态', 'doc_process_status', 103, 1, sysdate(), NULL, NULL, '流程状态列表');
INSERT INTO sys_dict_data (dict_code, tenant_id, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, create_dept, create_by, create_time, update_by, update_time, remark)
VALUES (79, '000000', 1, '待启动', 'pending', 'doc_process_status', '', 'info', 'Y', 103, 1, sysdate(), NULL, NULL, '待启动');
INSERT INTO sys_dict_data (dict_code, tenant_id, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, create_dept, create_by, create_time, update_by, update_time, remark)
VALUES (80, '000000', 2, '运行中', 'running', 'doc_process_status', '', 'warning', 'N', 103, 1, sysdate(), NULL, NULL, '运行中');
INSERT INTO sys_dict_data (dict_code, tenant_id, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, create_dept, create_by, create_time, update_by, update_time, remark)
VALUES (81, '000000', 3, '已完成', 'completed', 'doc_process_status', '', 'success', 'N', 103, 1, sysdate(), NULL, NULL, '已完成');

-- 项目成员角色
INSERT INTO sys_dict_type (dict_id, tenant_id, dict_name, dict_type, create_dept, create_by, create_time, update_by, update_time, remark)
VALUES (23, '000000', '项目成员角色', 'doc_member_role', 103, 1, sysdate(), NULL, NULL, '项目成员角色列表');
INSERT INTO sys_dict_data (dict_code, tenant_id, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, create_dept, create_by, create_time, update_by, update_time, remark)
VALUES (82, '000000', 1, '项目负责人', 'owner', 'doc_member_role', '', 'danger', 'Y', 103, 1, sysdate(), NULL, NULL, '项目负责人');
INSERT INTO sys_dict_data (dict_code, tenant_id, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, create_dept, create_by, create_time, update_by, update_time, remark)
VALUES (83, '000000', 2, '项目编辑', 'editor', 'doc_member_role', '', 'warning', 'N', 103, 1, sysdate(), NULL, NULL, '项目编辑');
INSERT INTO sys_dict_data (dict_code, tenant_id, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, create_dept, create_by, create_time, update_by, update_time, remark)
VALUES (84, '000000', 3, '项目只读', 'viewer', 'doc_member_role', '', 'info', 'N', 103, 1, sysdate(), NULL, NULL, '项目只读');

-- 插件执行状态
INSERT INTO sys_dict_type (dict_id, tenant_id, dict_name, dict_type, create_dept, create_by, create_time, update_by, update_time, remark)
VALUES (24, '000000', '插件执行状态', 'doc_plugin_execution_status', 103, 1, sysdate(), NULL, NULL, '插件执行状态列表');
INSERT INTO sys_dict_data (dict_code, tenant_id, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, create_dept, create_by, create_time, update_by, update_time, remark)
VALUES (85, '000000', 1, '成功', 'success', 'doc_plugin_execution_status', '', 'success', 'Y', 103, 1, sysdate(), NULL, NULL, '成功');
INSERT INTO sys_dict_data (dict_code, tenant_id, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, create_dept, create_by, create_time, update_by, update_time, remark)
VALUES (86, '000000', 2, '失败', 'failed', 'doc_plugin_execution_status', '', 'danger', 'N', 103, 1, sysdate(), NULL, NULL, '失败');

-- 插件执行日志权限
insert into sys_menu values('3041', '日志查询', '3040', '1', '#', '', '', 1, 0, 'F', '0', '0', 'docman:plugin:list', '#', 103, 1, sysdate(), null, null, '');
-- 插件手动触发权限
insert into sys_menu values('3044', '手动触发', '3040', '2', '#', '', '', 1, 0, 'F', '0', '0', 'docman:plugin:trigger', '#', 103, 1, sysdate(), null, null, '');

-- 归档下载权限
insert into sys_menu values('3034', '归档下载', '3030', '3', '#', '', '', 1, 0, 'F', '0', '0', 'docman:archive:download', '#', 103, 1, sysdate(), null, null, '');

-- 项目成员管理页面菜单（挂在项目管理 3001 下，隐藏路由）
insert into sys_menu values('3006', '成员管理', '3001', '1', 'member/:projectId', 'docman/member/index', '', 1, 1, 'C', '1', '0', 'docman:project:query', '#', 103, 1, sysdate(), null, null, '');
-- 成员管理按钮权限
insert into sys_menu values('3007', '成员查询', '3006', '1', '#', '', '', 1, 0, 'F', '0', '0', 'docman:project:query', '#', 103, 1, sysdate(), null, null, '');
insert into sys_menu values('3008', '成员新增', '3006', '2', '#', '', '', 1, 0, 'F', '0', '0', 'docman:project:edit', '#', 103, 1, sysdate(), null, null, '');
insert into sys_menu values('3009', '成员删除', '3006', '3', '#', '', '', 1, 0, 'F', '0', '0', 'docman:project:edit', '#', 103, 1, sysdate(), null, null, '');
-- project/index.vue 中用到的跳转权限按钮（成员/流程/归档入口）
insert into sys_menu values('3023', '流程查看', '3020', '3', '#', '', '', 1, 0, 'F', '0', '0', 'docman:process:query', '#', 103, 1, sysdate(), null, null, '');
insert into sys_menu values('3035', '归档查看', '3030', '4', '#', '', '', 1, 0, 'F', '0', '0', 'docman:archive:query', '#', 103, 1, sysdate(), null, null, '');

-- ==========================================
-- 节点截止日期记录表
-- ==========================================
CREATE TABLE doc_node_deadline (
    id                  BIGINT          NOT NULL COMMENT '主键',
    process_instance_id BIGINT          NOT NULL COMMENT '流程实例ID',
    node_code           VARCHAR(100)    NOT NULL COMMENT '节点编码',
    project_id          BIGINT          NOT NULL COMMENT '项目ID',
    duration_days       INT             NOT NULL DEFAULT 0 COMMENT '节点时限(天)',
    deadline            DATE            NOT NULL COMMENT '截止日期',
    reminder_count      INT             NOT NULL DEFAULT 0 COMMENT '已提醒次数',
    last_reminded_at    DATETIME        NULL COMMENT '最后提醒时间',
    create_time         DATETIME        NULL COMMENT '创建时间',
    update_time         DATETIME        NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_instance_node (process_instance_id, node_code),
    KEY idx_project (project_id),
    KEY idx_deadline (deadline)
) ENGINE=InnoDB COMMENT='节点截止日期记录';

-- 查看节点截止日期列表权限（按钮级别）
INSERT INTO sys_menu VALUES (3050, '节点截止查看', 3000, 1, '', '', '', 1, 0, 'F', '0', '0', 'docman:nodedeadline:query', '#', 103, 1, NOW(), NULL, NULL, '');
-- 修改节点截止日期权限（按钮级别）
INSERT INTO sys_menu VALUES (3051, '节点截止修改', 3000, 2, '', '', '', 1, 0, 'F', '0', '0', 'docman:nodedeadline:edit', '#', 103, 1, NOW(), NULL, NULL, '');

-- 仪表盘菜单
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, menu_type, perms, icon, create_by, create_time)
VALUES (3052, '仪表盘', 3000, 0, 'dashboard', 'docman/dashboard/index', 'C', 'docman:project:list', 'dashboard', 1, NOW());

-- 修复 doc_node_deadline 唯一约束
SET @idx_exists := (
    SELECT COUNT(1)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'doc_node_deadline'
      AND index_name = 'idx_instance_node'
);
SET @drop_idx_sql := IF(@idx_exists > 0, 'ALTER TABLE doc_node_deadline DROP INDEX idx_instance_node', 'SELECT 1');
PREPARE stmt FROM @drop_idx_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @uk_exists := (
    SELECT COUNT(1)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'doc_node_deadline'
      AND index_name = 'uk_instance_node'
);
SET @add_uk_sql := IF(@uk_exists = 0, 'ALTER TABLE doc_node_deadline ADD UNIQUE KEY uk_instance_node (process_instance_id, node_code)', 'SELECT 1');
PREPARE stmt FROM @add_uk_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
