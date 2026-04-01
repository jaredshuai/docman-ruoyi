# ruoyi-docman - Architecture

**Date:** 2026-04-01  
**Type:** Backend Module  
**Architecture:** Layered backend with application-domain-infrastructure separation

## 模块定位

`ruoyi-docman` 是一个面向“项目文档生产与归档”的后端业务模块，负责把项目、成员、流程、节点上下文、插件执行、文档产物、归档包和提醒能力组织成一条完整业务链。模块本身不独立启动，而是作为 `RuoYi-Vue-Plus` 后端中的业务子模块运行。

## 架构分层

### 1. 接口层

- `controller/*Controller.java`
- 负责权限校验注解、参数绑定、HTTP 响应包装
- 暴露 8 组 API：项目、成员、流程、文档、归档、看板、插件、节点时限

### 2. 应用层

- `application/service/*ApplicationService.java`
- 负责编排命令/查询场景
- 典型职责：
  - 项目和成员的增删改查编排
  - 流程绑定与启动
  - 文档上传/下载/预览票据生成
  - 节点完成事件触发插件链
  - 归档下载和执行日志查询

### 3. 领域层

- `domain/entity`: 映射 `doc_*` 业务表
- `domain/enums`: 项目、流程、文档、归档、插件执行等状态枚举
- `domain/service`: 状态机、权限策略、路径解析、归档快照等规则

领域层的核心价值在于把“流程可否启动、文档可否归档、角色能否执行某动作”这类规则从控制器和 MyBatis 查询中抽出来，避免业务散落。

### 4. 服务实现层

- `service/impl`
- 负责事务、缓存、CRUD、聚合查询、归档落库、上传下载和提醒发送
- 直接连接 mapper、port、domain service

### 5. 基础设施/适配层

- `infrastructure/storage`: 对接 OSS/对象存储，并允许本地降级
- `infrastructure/workflow`: 对接 Warm-Flow 引擎
- `infrastructure/notify`: SSE 系统消息推送
- `infrastructure/ai` / `knowledge`: AI 生成与知识检索适配
- `infrastructure/log`: 插件执行日志持久化

### 6. 插件运行时

- `plugin/DocumentPlugin`: SPI 协议
- `plugin/PluginRegistry`: 启动时注册插件
- `plugin/runtime/PluginExecutor`: 执行插件并记录日志
- `plugin/impl/*`: 内置插件实现，如 AI 生成、数据抽取、Excel 填充

## 关键业务能力

### 项目与成员管理

- `DocProjectController` + `DocProjectApplicationService` + `DocProjectServiceImpl`
- 管理项目元数据、成员列表、NAS 根路径和缓存失效
- `DocProjectAccessServiceImpl` 使用 Sa-Token 当前用户和 Redis 缓存维护“可访问项目列表 / 项目角色”

### 流程绑定与启动

- `DocProcessController` + `DocProcessApplicationService` + `DocProcessServiceImpl`
- `doc_process_config` 负责项目与 Warm-Flow 定义/实例的绑定关系
- 启动流程前通过 `DocProcessStateMachine` 校验状态是否合法

### 节点插件执行

- Warm-Flow 节点完成时，`DocmanNodeListener` 触发 `DocWorkflowNodeApplicationService`
- 应用服务读取节点扩展配置中的 `plugins`、`archiveFolderName`
- 通过 `NodeContextReader` 提供跨节点上下文读取
- `PluginExecutor` 执行插件并把执行日志写入 `doc_plugin_execution_log`
- 插件产出的文件再写回 `doc_document_record`

### 文档上传、下载与预览

- 上传入口在 `DocDocumentRecordController#upload`
- 文档优先写入 `DocumentStoragePort`，失败时降级到本地目录
- 在线预览通过 `DocDocumentViewerApplicationService` 生成 Redis 临时票据，再拼接到 viewer URL
- 预览内容读取时会再次校验票据和项目关系，避免直接暴露存储路径

### 项目归档

- `DocArchiveServiceImpl` 汇总项目下文档记录
- 通过 `DocArchiveDomainService` 构造归档 manifest
- 使用 EasyExcel 生成归档清单 Excel
- 文档状态由 `generated -> archived`，项目状态由 `active -> archived`
- 归档下载由 `DocArchiveApplicationService` 直接流式输出 ZIP

### 看板与提醒

- `DocDashboardServiceImpl` 提供总览、待办、项目进度、逾期提醒、插件统计
- `DocumentReminderJob` / `NodeDeadlineReminderJob` 负责定时提醒
- `NasRetryJob` 负责补偿待创建 NAS 目录

## 核心运行链路

### 链路一：项目启动文档流程

1. 控制器接收项目绑定或启动请求
2. 应用服务调用 `IDocProcessService`
3. `DocProcessServiceImpl` 校验权限和状态
4. `ProcessEnginePort` 调用 Warm-Flow 启动流程
5. 流程实例 ID 回写 `doc_process_config`

### 链路二：节点完成触发插件

1. Warm-Flow 产生节点完成事件
2. `DocmanNodeListener` 转发到 `DocWorkflowNodeApplicationService`
3. 读取节点扩展 JSON，解析插件绑定
4. 构建 `PluginContext`
5. `PluginExecutor` 执行插件并落执行日志
6. 生成的文件登记到 `doc_document_record`
7. 如流程结束，则把流程配置状态推进为 `completed`

### 链路三：文档归档

1. 发起归档请求
2. 服务层加载项目全部文档并校验都可归档
3. 生成归档 manifest 与清单 Excel
4. 写入 `doc_archive_package`
5. 批量更新文档状态和项目状态

## 数据架构

本模块围绕 8 张 `doc_*` 业务表展开：

- `doc_project`: 项目主表
- `doc_project_member`: 项目成员
- `doc_process_config`: 项目与流程绑定
- `doc_document_record`: 文档记录与产物
- `doc_archive_package`: 归档包
- `doc_node_context`: 节点上下文
- `doc_node_deadline`: 节点时限与提醒
- `doc_plugin_execution_log`: 插件执行日志

关系上以 `doc_project` 为聚合根，向下关联成员、文档、流程配置、归档包、节点时限和插件日志；`doc_node_context` 与 `processInstanceId + nodeCode` 组合形成工作流上下文视图。

## 安全与权限

- 控制器统一使用 `@SaCheckPermission`
- 项目级权限通过 `DocProjectAccessServiceImpl` + `DocProjectPermissionPolicy` 判断
- 预览内容接口虽然使用 `@SaIgnore`，但仍依赖 Redis 票据和项目归属校验
- 关键状态流转通过状态机类显式校验，避免非法跳转

## 外部依赖与边界

- **宿主系统:** `ruoyi-admin`
- **认证与授权:** Sa-Token
- **持久化:** MyBatis-Plus + MyBatis XML
- **流程引擎:** Warm-Flow
- **缓存/票据:** Redis
- **对象存储:** `DocumentStoragePort`（OSS / 本地降级）
- **通知:** SSE
- **AI/知识检索:** HTTP 适配器

## 测试策略

测试文件按包结构覆盖：

- controller: 9 个测试
- application/service: 15 个测试
- service/impl: 12 个测试
- domain/service: 6 个测试
- infrastructure/job/listener/plugin/sql: 均有对应测试

这说明模块的测试重点不只是 CRUD，还覆盖了工作流、插件、SQL 契约和任务调度。

## 设计特点与风险点

### 设计特点

- 明确的 command/query 应用服务拆分
- 插件体系与工作流引擎解耦
- 状态机约束流程/文档/项目生命周期
- 存储端口支持 OSS 与本地降级双路径

### 风险点

- 模块入口分散于 HTTP、任务和工作流事件，问题排查需要结合三类入口
- 预览和上传兼容本地降级路径，环境差异容易导致“开发可用 / 线上不可用”
- 节点扩展配置依赖约定字段，配置错误会直接影响插件触发

---

_Generated using BMAD Method `document-project` workflow_
