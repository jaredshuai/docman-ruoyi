# Docman API 性能优化记录

## 背景

2026-03-25~26 对 docman 模块核心 API 进行性能优化，主要解决：
- 项目列表 N+1 查询问题
- 成员变更后缓存不一致导致的安全风险（移除成员后仍可访问项目）
- 插件执行日志列表传输大量 LONGTEXT 字段

**效果数据来源**：本地 Docker 环境、experiments.tsv、样本量 10-20 次请求。生产环境需单独复测。

## 分支说明

- `main`：发布线
- `autoresearch/perf-mar25`：本次优化开发分支，与 main 同步

## 已优化 API

| API | 优化措施 | 效果（本地 Docker） |
|-----|---------|-------------------|
| GET /docman/project/list | JOIN 查询 + Redis 缓存 | 38.7ms → 21.3ms（样本量 20 次，约 45% 提升） |
| GET /docman/plugin/execution/list | 排除 LONGTEXT 字段 | 减少 request_snapshot、result_snapshot 两个大字段传输 |
| GET /docman/plugin/execution/{id} | 新增详情 API | 按需获取完整快照字段 |

## 缓存一致性修复

### 缓存名称

| 常量名 | 缓存键前缀 | TTL | 说明 |
|--------|-----------|-----|------|
| `USER_ACCESSIBLE_PROJECTS` | `docman_user_accessible_projects` | 5 分钟 | 用户可访问项目 ID 列表 |
| `USER_PROJECT_ROLE` | `docman_user_project_role` | 10 分钟 | 用户在项目中的角色 |

### 失效策略

| 操作 | 实现方式 | 失效缓存 |
|------|---------|---------|
| 读路径 | `@Cacheable` 注解 | — |
| 添加成员 `addMember` | `@CacheEvict(cacheNames=USER_ACCESSIBLE_PROJECTS, key=新成员userId)` | 仅新成员的可访问项目缓存 |
| 移除成员 `removeMember` | `IDocProjectAccessService.evictAccessibleProjectsCache` + `evictProjectRoleCache`（内部 `RedisUtils.deleteObject`） | 被移除用户的两个缓存 |
| 创建项目 `insertProject` | `evictAccessibleProjectsCache`（批量） | 所有新增成员的可访问项目缓存 |
| 删除项目 `deleteByIds` | `evictAccessibleProjectsCache` + `evictProjectRoleCache`（批量） | 所有受影响用户的两个缓存 |

## 待观察 API

| API | 当前状态 | 本轮处理 |
|-----|---------|---------|
| GET /docman/document/list | 基线约 15ms（本地 Docker，样本量 10 次） | 本轮未做代码层优化；索引已包含在脚本中 |

## 索引脚本

路径：`script/sql/perf_index_doc_project.sql`

**语法说明**：标准 MySQL 不支持 `CREATE INDEX IF NOT EXISTS`（MariaDB 支持）。若索引已存在会报 `Duplicate key name`，可忽略或先 `DROP INDEX` 再执行。

### 项目表 `doc_project`

| 索引名 | 字段 | 用途 |
|--------|------|------|
| `idx_project_list` | `create_time DESC, customer_type, business_type` | 项目列表排序与筛选 |
| `idx_project_owner` | `owner_id` | 按拥有者查询项目 |
| `idx_project_status` | `status` | 按状态查询项目 |

### 项目成员表 `doc_project_member`

| 索引名 | 字段 | 用途 |
|--------|------|------|
| `idx_member_user` | `user_id` | 按用户查询可访问项目 |
| `idx_member_project_user` | `project_id, user_id` | 按项目+用户查询角色 |

### 文档记录表 `doc_document_record`

| 索引名 | 字段 | 用途 |
|--------|------|------|
| `idx_document_list` | `project_id, nas_path` | 文档列表排序与筛选 |
| `idx_document_status` | `project_id, status` | 按项目与状态查询文档 |

## 索引部署记录

| 环境 | 数据库 | 状态 | 确认日期 | 备注 |
|------|--------|------|---------|------|
| 开发 | 10.34.200.174/app_db | ✅ 已部署 | 2026-03-26 | 7 个索引已存在 |
| 生产 | 10.34.200.173/app_db | ✅ 已部署 | 2026-03-26 | 7 个索引已存在 |