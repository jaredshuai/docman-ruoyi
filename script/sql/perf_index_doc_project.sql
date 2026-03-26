-- 性能优化索引
-- 为 docman 模块的核心查询添加复合索引

-- ========== 项目表索引 ==========

-- 项目列表查询优化
-- 支持 create_time DESC, customer_type, business_type 的常见查询组合
CREATE INDEX IF NOT EXISTS idx_project_list ON doc_project(create_time DESC, customer_type, business_type);

-- 项目成员查询优化
-- 支持按 owner_id 查询用户的项目
CREATE INDEX IF NOT EXISTS idx_project_owner ON doc_project(owner_id);

-- 项目状态查询优化
CREATE INDEX IF NOT EXISTS idx_project_status ON doc_project(status);

-- ========== 项目成员表索引 ==========

-- 支持按用户查询可访问的项目（listAccessibleProjectIds）
CREATE INDEX IF NOT EXISTS idx_member_user ON doc_project_member(user_id);

-- 支持按项目+用户查询角色（getCurrentRole）
CREATE INDEX IF NOT EXISTS idx_member_project_user ON doc_project_member(project_id, user_id);

-- ========== 文档记录表索引 ==========

-- 文档列表查询优化
-- 支持按 project_id 和 nas_path 的查询
CREATE INDEX IF NOT EXISTS idx_document_list ON doc_document_record(project_id, nas_path);

-- 文档状态查询优化
CREATE INDEX IF NOT EXISTS idx_document_status ON doc_document_record(project_id, status);