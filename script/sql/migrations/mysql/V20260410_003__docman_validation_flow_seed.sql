SET NAMES utf8mb4;

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
