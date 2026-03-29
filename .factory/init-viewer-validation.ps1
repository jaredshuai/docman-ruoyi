$ErrorActionPreference = 'Stop'

$repoRoot = 'D:/codespace/docman-ruoyi'
$runtimeRoot = "$repoRoot/.factory/runtime"
$uploadRoot = "$runtimeRoot/docman-upload"
$sampleDir = "$uploadRoot/viewer-validation"
$sampleFile = "$sampleDir/viewer-validation.txt"

[System.IO.Directory]::CreateDirectory($sampleDir) | Out-Null
[System.IO.File]::WriteAllText($sampleFile, 'viewer preview validation sample')

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

$seedSql = @"
INSERT INTO doc_project (
  id, name, customer_type, business_type, document_category, status, owner_id,
  nas_base_path, nas_dir_status, remark, create_dept, create_by, create_time, del_flag
)
SELECT
  9100001, 'Viewer Validation Project', 'telecom', 'pipeline', 'internal', 'active', 1,
  '/viewer-validation', 'created', 'viewer validation seed', 103, 1, NOW(), '0'
WHERE NOT EXISTS (SELECT 1 FROM doc_project WHERE id = 9100001);

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
