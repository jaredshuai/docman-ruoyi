-- Docman 普通用户工作台 Phase 1
-- 说明：
-- 1. 新增普通用户角色、工作台菜单
-- 2. 复用现有“我的任务”菜单
-- 3. 通过功能权限开放文档/流程/归档页面访问，但不暴露管理员菜单

SET @workspace_role_key := 'docman_user';
SET @workspace_root_menu_id := 3060;
SET @workspace_home_menu_id := 3061;
SET @workspace_project_menu_id := 3062;
SET @workspace_document_menu_id := 3063;
SET @workspace_process_menu_id := 3064;
SET @workspace_archive_menu_id := 3065;

INSERT INTO sys_role (
    role_id,
    role_name,
    role_key,
    role_sort,
    data_scope,
    status,
    del_flag,
    create_by,
    create_time,
    remark
)
SELECT
    COALESCE((SELECT MAX(role_id) FROM sys_role), 0) + 1,
    'Docman普通用户',
    @workspace_role_key,
    10,
    5,
    '0',
    '0',
    1,
    NOW(),
    'Docman 普通用户工作台'
WHERE NOT EXISTS (
    SELECT 1 FROM sys_role WHERE role_key = @workspace_role_key
);

SET @workspace_role_id := (
    SELECT role_id FROM sys_role WHERE role_key = @workspace_role_key LIMIT 1
);

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, menu_type, perms, icon, create_by, create_time)
SELECT @workspace_root_menu_id, '工作台', 0, 4, 'workspace', NULL, 'M', '', 'dashboard', 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = @workspace_root_menu_id);

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, menu_type, perms, icon, create_by, create_time)
SELECT @workspace_home_menu_id, '工作台首页', @workspace_root_menu_id, 1, 'home', 'workspace/home/index', 'C', 'docman:dashboard:todo-summary', 'dashboard', 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = @workspace_home_menu_id);

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, menu_type, perms, icon, create_by, create_time)
SELECT @workspace_project_menu_id, '我的项目', @workspace_root_menu_id, 2, 'project', 'workspace/project/index', 'C', 'docman:project:my', 'tree', 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = @workspace_project_menu_id);

INSERT INTO sys_menu (
    menu_id, menu_name, parent_id, order_num, path, component,
    menu_type, perms, icon, visible, create_by, create_time
)
SELECT
    @workspace_document_menu_id, '项目文档', @workspace_root_menu_id, 3, 'document', 'docman/document/index',
    'C', 'docman:document:list', 'list', '1', 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = @workspace_document_menu_id);

INSERT INTO sys_menu (
    menu_id, menu_name, parent_id, order_num, path, component,
    menu_type, perms, icon, visible, create_by, create_time
)
SELECT
    @workspace_process_menu_id, '项目流程', @workspace_root_menu_id, 4, 'process', 'docman/process/index',
    'C', 'docman:process:query', 'cascader', '1', 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = @workspace_process_menu_id);

INSERT INTO sys_menu (
    menu_id, menu_name, parent_id, order_num, path, component,
    menu_type, perms, icon, visible, create_by, create_time
)
SELECT
    @workspace_archive_menu_id, '项目归档', @workspace_root_menu_id, 5, 'archive', 'docman/archive/index',
    'C', 'docman:archive:query', 'zip', '1', 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = @workspace_archive_menu_id);

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT @workspace_role_id, menu_id
FROM (
    SELECT @workspace_root_menu_id AS menu_id
    UNION ALL SELECT @workspace_home_menu_id
    UNION ALL SELECT @workspace_project_menu_id
    UNION ALL SELECT @workspace_document_menu_id
    UNION ALL SELECT @workspace_process_menu_id
    UNION ALL SELECT @workspace_archive_menu_id
    UNION ALL SELECT 3011
    UNION ALL SELECT 3012
    UNION ALL SELECT 3034
    UNION ALL SELECT 11618
    UNION ALL SELECT 11619
    UNION ALL SELECT 11629
    UNION ALL SELECT 11632
    UNION ALL SELECT 11633
) AS menu_ids
WHERE NOT EXISTS (
    SELECT 1
    FROM sys_role_menu rm
    WHERE rm.role_id = @workspace_role_id
      AND rm.menu_id = menu_ids.menu_id
);
