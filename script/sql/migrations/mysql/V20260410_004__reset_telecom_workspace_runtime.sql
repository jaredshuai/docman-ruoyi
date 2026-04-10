SET NAMES utf8mb4;

DELETE FROM doc_node_context
WHERE project_id IN (
  SELECT id
  FROM doc_project
  WHERE project_type_code = 'telecom'
);

DELETE FROM doc_project_node_task_runtime
WHERE project_id IN (
  SELECT id
  FROM doc_project
  WHERE project_type_code = 'telecom'
);

DELETE FROM doc_project_runtime
WHERE project_id IN (
  SELECT id
  FROM doc_project
  WHERE project_type_code = 'telecom'
);
