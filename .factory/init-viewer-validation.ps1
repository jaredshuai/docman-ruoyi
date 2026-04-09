$ErrorActionPreference = 'Stop'

$repoRoot = 'D:/codespace/docman-ruoyi'
$runtimeRoot = "$repoRoot/.factory/runtime"
$uploadRoot = "$runtimeRoot/docman-upload"
$sampleDir = "$uploadRoot/viewer-validation"
$sampleFile = "$sampleDir/viewer-validation.txt"

[System.IO.Directory]::CreateDirectory($sampleDir) | Out-Null
[System.IO.File]::WriteAllText($sampleFile, 'viewer preview validation sample')

# 本地验证统一要求 docman-redis 使用固定密码，避免 Redisson 在无密码实例上发送 AUTH 失败。
$redisPassword = 'ruoyi123'
docker exec docman-redis redis-cli -a $redisPassword ping | Out-Null
$redisAuthOk = ($LASTEXITCODE -eq 0) -and $?
if (-not $redisAuthOk) {
  docker exec docman-redis redis-cli CONFIG SET requirepass $redisPassword | Out-Null
  if (($LASTEXITCODE -ne 0) -or (-not $?)) {
    throw 'Failed to normalize docman-redis password to ruoyi123.'
  }
}

$baseTableCount = docker exec docman-mysql mysql -uroot -proot -Nse "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='ry-vue';"
if ($baseTableCount -eq '0') {
  docker cp "$repoRoot/script/sql/ry_vue_5.X.sql" "docman-mysql:/tmp/ry_vue_5.X.sql"
  docker exec docman-mysql sh -lc "mysql --default-character-set=utf8mb4 -uroot -proot ry-vue < /tmp/ry_vue_5.X.sql"
}

$docmanTableCount = docker exec docman-mysql mysql -uroot -proot -Nse "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='ry-vue' AND table_name='doc_project';"
if ($docmanTableCount -eq '0') {
  docker cp "$repoRoot/script/sql/ry_docman.sql" "docman-mysql:/tmp/ry_docman.sql"
  docker exec docman-mysql sh -lc "mysql --default-character-set=utf8mb4 -uroot -proot ry-vue < /tmp/ry_docman.sql"
}

$workflowTableCount = docker exec docman-mysql mysql -uroot -proot -Nse "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='ry-vue' AND table_name='flow_node';"
if ($workflowTableCount -eq '0') {
  docker cp "$repoRoot/script/sql/ry_workflow.sql" "docman-mysql:/tmp/ry_workflow.sql"
  docker exec docman-mysql sh -lc "mysql --default-character-set=utf8mb4 -uroot -proot ry-vue < /tmp/ry_workflow.sql"
}

$workflowSeedSql = @"
INSERT INTO flow_definition (
  id, flow_code, flow_name, model_value, category, version, is_publish, form_custom, form_path, activity_status,
  listener_type, listener_path, ext, create_time, create_by, update_time, update_by, del_flag, tenant_id
)
SELECT
  9100101, 'docman_validation_flow', 'Docman Validation Flow', 'CLASSICS', 'docman', 'v1', 1, 'N', NULL, 1,
  NULL, NULL, NULL, NOW(), '1', NOW(), '1', '0', '000000'
WHERE NOT EXISTS (SELECT 1 FROM flow_definition WHERE id = 9100101);

INSERT INTO flow_node (
  id, node_type, definition_id, node_code, node_name, permission_flag, node_ratio, coordinate, any_node_skip,
  listener_type, listener_path, form_custom, form_path, version, create_time, create_by, update_time, update_by, ext, del_flag, tenant_id
)
SELECT
  9100102, 0, 9100101, 'start', '开始', NULL, NULL, NULL, NULL,
  NULL, NULL, 'N', NULL, 'v1', NOW(), '1', NOW(), '1', NULL, '0', '000000'
WHERE NOT EXISTS (SELECT 1 FROM flow_node WHERE id = 9100102);

INSERT INTO flow_node (
  id, node_type, definition_id, node_code, node_name, permission_flag, node_ratio, coordinate, any_node_skip,
  listener_type, listener_path, form_custom, form_path, version, create_time, create_by, update_time, update_by, ext, del_flag, tenant_id
)
SELECT
  9100103, 2, 9100101, 'end', '结束', NULL, NULL, NULL, NULL,
  NULL, NULL, 'N', NULL, 'v1', NOW(), '1', NOW(), '1', NULL, '0', '000000'
WHERE NOT EXISTS (SELECT 1 FROM flow_node WHERE id = 9100103);

UPDATE flow_node
SET ext = '{"archiveFolderName":"exports","plugins":[{"pluginId":"telecom-export-text-mock","config":{}}]}'
WHERE id = 9100103;

INSERT INTO flow_skip (
  id, definition_id, now_node_code, now_node_type, next_node_code, next_node_type, skip_name, skip_type, skip_condition,
  coordinate, create_time, create_by, update_time, update_by, del_flag, tenant_id
)
SELECT
  9100104, 9100101, 'start', 0, 'end', 2, '提交', 'PASS', NULL,
  NULL, NOW(), '1', NOW(), '1', '0', '000000'
WHERE NOT EXISTS (SELECT 1 FROM flow_skip WHERE id = 9100104);
"@

$workflowSeedSql | docker exec -i docman-mysql mysql --default-character-set=utf8mb4 -uroot -proot ry-vue

function Ensure-DocProjectColumn {
  param(
    [Parameter(Mandatory = $true)][string]$ColumnName,
    [Parameter(Mandatory = $true)][string]$ColumnDefinition
  )

  $columnExists = docker exec docman-mysql mysql -uroot -proot -Nse "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema='ry-vue' AND table_name='doc_project' AND column_name='$ColumnName';"
  if ($columnExists -eq '0') {
    @"
USE `ry-vue`;
ALTER TABLE doc_project ADD COLUMN $ColumnName $ColumnDefinition;
"@ | docker exec -i docman-mysql mysql --default-character-set=utf8mb4 -uroot -proot

    if (($LASTEXITCODE -ne 0) -or (-not $?)) {
      throw "Failed to add doc_project.$ColumnName for local validation."
    }
  }
}

Ensure-DocProjectColumn -ColumnName 'dianxin_code' -ColumnDefinition "VARCHAR(200) NULL COMMENT '电信编号' AFTER name"
Ensure-DocProjectColumn -ColumnName 'xiangyun_code' -ColumnDefinition "VARCHAR(200) NULL COMMENT '翔云编号' AFTER dianxin_code"
Ensure-DocProjectColumn -ColumnName 'price' -ColumnDefinition "DECIMAL(10,2) NULL COMMENT '项目金额' AFTER xiangyun_code"
Ensure-DocProjectColumn -ColumnName 'project_type_code' -ColumnDefinition "VARCHAR(64) NULL COMMENT '项目类型编码' AFTER price"
Ensure-DocProjectColumn -ColumnName 'customer_name' -ColumnDefinition "VARCHAR(200) NULL COMMENT '客户名称' AFTER status"
Ensure-DocProjectColumn -ColumnName 'dianxin_initiation_time' -ColumnDefinition "DATETIME NULL COMMENT '电信立项时间' AFTER owner_id"
Ensure-DocProjectColumn -ColumnName 'start_time' -ColumnDefinition "DATETIME NULL COMMENT '计划开工时间' AFTER dianxin_initiation_time"
Ensure-DocProjectColumn -ColumnName 'end_time' -ColumnDefinition "DATETIME NULL COMMENT '计划完工时间' AFTER start_time"

docker exec docman-redis sh -lc "redis-cli -a $redisPassword DEL '000000:docman_user_accessible_projects' 'redisson__idle__set:{000000:docman_user_accessible_projects}' 'redisson__timeout__set:{000000:docman_user_accessible_projects}' >/dev/null"
docker exec docman-redis sh -lc "redis-cli -a $redisPassword --scan --pattern '000000:docman_user_project_role*' | xargs -r redis-cli -a $redisPassword DEL >/dev/null"

$seedSql = @"
INSERT INTO doc_project (
  id, name, dianxin_code, xiangyun_code, price, project_type_code, customer_type, business_type, document_category, status, customer_name, owner_id, dianxin_initiation_time, start_time, end_time,
  nas_base_path, nas_dir_status, remark, create_dept, create_by, create_time, del_flag
)
SELECT
  9100001, 'Viewer Validation Project', 'DX-VALID-001', 'XY-VALID-001', 12345.67, 'telecom', 'telecom', 'pipeline', 'internal', 'active', 'Validation Customer', 1, NOW(), NOW(), DATE_ADD(NOW(), INTERVAL 30 DAY),
  '/viewer-validation', 'created', 'viewer validation seed', 103, 1, NOW(), '0'
WHERE NOT EXISTS (SELECT 1 FROM doc_project WHERE id = 9100001);

UPDATE doc_project
SET dianxin_code = 'DX-VALID-001',
    xiangyun_code = 'XY-VALID-001',
    price = 12345.67,
    project_type_code = 'telecom',
    customer_name = 'Validation Customer',
    dianxin_initiation_time = COALESCE(dianxin_initiation_time, NOW()),
    start_time = COALESCE(start_time, NOW()),
    end_time = COALESCE(end_time, DATE_ADD(NOW(), INTERVAL 30 DAY))
WHERE id = 9100001;

INSERT INTO doc_project_member (id, project_id, user_id, role_type, create_time)
SELECT 9100002, 9100001, 1, 'owner', NOW()
WHERE NOT EXISTS (SELECT 1 FROM doc_project_member WHERE id = 9100002);

INSERT INTO doc_document_record (
  id, project_id, node_instance_id, plugin_id, source_type, file_name, nas_path, oss_id,
  status, generated_at, create_dept, create_by, create_time, del_flag
)
SELECT
  9100003, 9100001, NULL, NULL, 'upload', 'viewer-validation.txt', '/viewer-validation/viewer-validation.txt', NULL,
  'generated', NOW(), 103, 1, NOW(), '0'
WHERE NOT EXISTS (SELECT 1 FROM doc_document_record WHERE id = 9100003);
"@

$seedSql | docker exec -i docman-mysql mysql --default-character-set=utf8mb4 -uroot -proot ry-vue
