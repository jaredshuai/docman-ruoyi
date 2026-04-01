# ruoyi-docman - Data Models

**Date:** 2026-04-01  
**Scope:** `src/main/java/org/dromara/docman/domain/entity` + `src/main/resources/mapper/docman`

## 总览

`ruoyi-docman` 的持久化模型围绕“项目 -> 流程 -> 节点 -> 文档 -> 归档/提醒/插件日志”展开，核心业务表均以 `doc_` 前缀命名。

## 实体列表

### 1. `doc_project`

**Java Entity:** `DocProject`  
**Purpose:** 项目主表，充当聚合根。

**关键字段**

- `id`: 主键
- `name`: 项目名称
- `customerType`: 客户类型
- `businessType`: 业务类型
- `documentCategory`: 文档类别
- `status`: 项目状态，典型值 `active / archived`
- `ownerId`: 项目负责人
- `nasBasePath`: 项目文档存储根路径
- `nasDirStatus`: NAS 目录状态，典型值 `pending / created / failed`
- `remark`: 备注
- `delFlag`: 逻辑删除标记

### 2. `doc_project_member`

**Java Entity:** `DocProjectMember`  
**Purpose:** 项目成员及其角色。

**关键字段**

- `id`
- `projectId`
- `userId`
- `roleType`: 角色，典型值 `owner / editor / viewer`
- `createTime`

### 3. `doc_process_config`

**Java Entity:** `DocProcessConfig`  
**Purpose:** 项目与 Warm-Flow 定义/实例的绑定关系。

**关键字段**

- `id`
- `projectId`
- `definitionId`: Warm-Flow 流程定义 ID
- `instanceId`: Warm-Flow 流程实例 ID
- `status`: 典型值 `pending / running / completed`

### 4. `doc_document_record`

**Java Entity:** `DocDocumentRecord`  
**Purpose:** 记录上传文档和插件产物。

**关键字段**

- `id`
- `projectId`
- `nodeInstanceId`: 所属流程节点实例
- `pluginId`: 生成来源插件 ID；手动上传时为空
- `sourceType`: `plugin / upload / archive_manifest` 等
- `fileName`: 展示文件名
- `nasPath`: 实际存储路径
- `ossId`: 关联对象存储记录
- `status`: 典型值 `pending / running / generated / failed / archived / obsolete`
- `generatedAt`
- `archivedAt`
- `delFlag`: 逻辑删除标记

### 5. `doc_archive_package`

**Java Entity:** `DocArchivePackage`  
**Purpose:** 项目归档快照。

**关键字段**

- `id`
- `projectId`
- `archiveNo`
- `archiveVersion`
- `nasArchivePath`
- `manifest`: Jackson JSON 列，记录归档清单
- `snapshotChecksum`
- `requestedAt`
- `completedAt`
- `status`: `requested / generating / completed / failed`

### 6. `doc_node_context`

**Java Entity:** `DocNodeContext`  
**Purpose:** 工作流节点上下文快照，支持跨节点读取。

**关键字段**

- `id`
- `processInstanceId`
- `nodeCode`
- `projectId`
- `processVariables`: JSON
- `nodeVariables`: JSON
- `documentFacts`: JSON
- `unstructuredContent`: JSON
- `createTime`
- `updateTime`

### 7. `doc_node_deadline`

**Java Entity:** `DocNodeDeadline`  
**Purpose:** 节点截止日期和提醒计数。

**关键字段**

- `id`
- `processInstanceId`
- `nodeCode`
- `projectId`
- `durationDays`
- `deadline`
- `reminderCount`
- `lastRemindedAt`

### 8. `doc_plugin_execution_log`

**Java Entity:** `DocPluginExecutionLog`  
**Purpose:** 记录插件执行请求和结果快照。

**关键字段**

- `id`
- `projectId`
- `processInstanceId`
- `nodeCode`
- `pluginId`
- `pluginName`
- `status`: `success / failed`
- `costMs`
- `generatedFileCount`
- `errorMessage`
- `requestSnapshot`
- `resultSnapshot`

## 关系建模

### 项目聚合关系

- `doc_project (1) -> (N) doc_project_member`
- `doc_project (1) -> (N) doc_document_record`
- `doc_project (1) -> (N) doc_archive_package`
- `doc_project (1) -> (N) doc_node_deadline`
- `doc_project (1) -> (N) doc_plugin_execution_log`
- `doc_project (1) -> (N) doc_node_context`
- `doc_project (1) -> (0..1/N) doc_process_config`

### 流程相关关系

- `doc_process_config.definitionId` / `instanceId` 对接 Warm-Flow 外部表
- `doc_node_context.processInstanceId`
- `doc_node_deadline.processInstanceId`
- `doc_plugin_execution_log.processInstanceId`
- `doc_document_record.nodeInstanceId`

### 文档与归档关系

- 归档前：`doc_document_record.status = generated`
- 归档后：文档状态更新为 `archived`
- 归档清单保存到 `doc_archive_package.manifest`
- 归档清单 Excel 本身也会作为一条文档清单项进入归档 manifest

## 状态机与约束

### 项目状态

- `DocProjectStateMachine` 负责项目状态流转
- 归档时项目会从活动状态推进到归档状态

### 流程状态

- `DocProcessStateMachine` 控制 `pending -> running -> completed`
- 启动流程和监听流程结束都会经过状态机校验

### 文档状态

- `DocDocumentStateMachine` 控制文档能否归档
- 归档前会校验项目下文档是否都处于可归档状态

## MyBatis Mapper 与 SQL 特征

### Java Mapper

- `DocProjectMapper`
- `DocProjectMemberMapper`
- `DocProcessConfigMapper`
- `DocDocumentRecordMapper`
- `DocArchivePackageMapper`
- `DocNodeContextMapper`
- `DocNodeDeadlineMapper`
- `DocPluginExecutionLogMapper`
- `DocDashboardMapper`

### XML Mapper

- `DocProjectMapper.xml`: 当前用户可访问项目分页查询，使用 JOIN 消除 N+1
- 其余 XML 目前以补充查询或契约验证为主，复杂度不高

## 数据读写热点

### 读热点

- 项目列表与当前用户可访问项目
- 项目成员及当前用户角色
- 项目文档列表
- 看板统计和预警
- 插件执行日志分页

### 写热点

- 项目创建时自动创建成员与存储目录
- 文档上传/插件产出登记
- 流程绑定与实例启动
- 节点截止日期更新
- 项目归档批量更新文档状态

## 缓存与冗余

- `DocProjectAccessServiceImpl` 缓存用户可访问项目 ID 列表
- 同时缓存“用户在项目中的角色”
- 项目成员变更、项目删除后会显式失效缓存

## 数据建模观察

- `doc_project` 是明确的聚合根，表边界清晰
- `doc_node_context` 用 JSON 列承载工作流上下文，适合动态插件字段，但查询维度弱
- `doc_plugin_execution_log` 把请求/结果快照做成字符串存储，便于审计但需要注意体积增长
- `doc_document_record` 兼顾手动上传、插件产物和归档清单三类来源，是最核心的产物表

---

_Generated using BMAD Method `document-project` workflow_
