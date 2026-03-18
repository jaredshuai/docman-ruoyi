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
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, menu_type, perms, create_by, create_time)
VALUES (3040, '插件列表', 3000, 5, 'F', 'docman:plugin:list', 1, NOW());

-- 文档下载、删除权限
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, menu_type, perms, create_by, create_time)
VALUES (3012, '文档下载', 3010, 2, 'F', 'docman:document:download', 1, NOW());
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, menu_type, perms, create_by, create_time)
VALUES (3013, '文档删除', 3010, 3, 'F', 'docman:document:delete', 1, NOW());
