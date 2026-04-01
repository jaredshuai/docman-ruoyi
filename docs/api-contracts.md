# ruoyi-docman - API Contracts

**Date:** 2026-04-01  
**Scope:** `ruoyi-modules/ruoyi-docman/src/main/java/org/dromara/docman/controller`

## 说明

- 接口统一挂载在宿主后端中，由 `ruoyi-admin` 暴露
- 返回值以 `R<T>` 或 `TableDataInfo<T>` 为主
- 访问控制主要通过 `@SaCheckPermission` 完成
- 除在线预览内容读取接口外，所有入口默认要求登录态和权限

## 1. 项目管理

**Base Path:** `/docman/project`

### `GET /docman/project/list`

- **Permission:** `docman:project:list`
- **Purpose:** 分页查询项目列表
- **Query:** `DocProjectBo bo`, `PageQuery pageQuery`
- **Response:** `TableDataInfo<DocProjectVo>`

### `GET /docman/project/my`

- **Permission:** `docman:project:my`
- **Purpose:** 查询当前用户可见的项目列表
- **Query:** `DocProjectBo bo`
- **Response:** `R<List<DocProjectVo>>`

### `GET /docman/project/{id}`

- **Permission:** `docman:project:query`
- **Purpose:** 查询单个项目详情及当前用户角色
- **Path:** `id`
- **Response:** `R<DocProjectVo>`

### `POST /docman/project`

- **Permission:** `docman:project:add`
- **Purpose:** 创建项目
- **Body:** `DocProjectBo`
- **Response:** `R<Long>`，返回项目 ID

### `PUT /docman/project`

- **Permission:** `docman:project:edit`
- **Purpose:** 更新项目
- **Body:** `DocProjectBo`，必须包含 `id`
- **Response:** `R<Void>`

### `DELETE /docman/project/{ids}`

- **Permission:** `docman:project:remove`
- **Purpose:** 批量删除项目
- **Path:** `ids`，支持列表
- **Response:** `R<Void>`

## 2. 项目成员管理

**Base Path:** `/docman/project/{projectId}/member`

### `GET /docman/project/{projectId}/member`

- **Permission:** `docman:project:query`
- **Purpose:** 查询项目成员列表
- **Path:** `projectId`
- **Response:** `R<List<DocProjectMemberVo>>`

### `POST /docman/project/{projectId}/member`

- **Permission:** `docman:project:edit`
- **Purpose:** 添加项目成员
- **Path:** `projectId`
- **Body:** `DocProjectMemberBo`
- **Response:** `R<Void>`

### `DELETE /docman/project/{projectId}/member/{userId}`

- **Permission:** `docman:project:edit`
- **Purpose:** 移除项目成员
- **Path:** `projectId`, `userId`
- **Response:** `R<Void>`

## 3. 流程配置与启动

**Base Path:** `/docman/process`

### `POST /docman/process/bind`

- **Permission:** `docman:process:bind`
- **Purpose:** 为项目绑定 Warm-Flow 定义
- **Query:** `projectId`, `definitionId`
- **Response:** `R<Void>`

### `POST /docman/process/start/{projectId}`

- **Permission:** `docman:process:start`
- **Purpose:** 启动指定项目的流程实例
- **Path:** `projectId`
- **Response:** `R<Long>`，返回流程实例 ID

### `GET /docman/process/{projectId}`

- **Permission:** `docman:process:query`
- **Purpose:** 查询项目当前流程配置
- **Path:** `projectId`
- **Response:** `R<DocProcessConfigVo>`

### `GET /docman/process/definitions`

- **Permission:** `docman:process:query`
- **Purpose:** 查询可绑定的流程定义列表
- **Response:** `R<List<Map<String, Object>>>`

## 4. 文档记录、上传下载与在线预览

**Base Path:** `/docman/document`

### `GET /docman/document/list`

- **Permission:** `docman:document:list`
- **Purpose:** 分页查询项目文档记录
- **Query:** `projectId`, `PageQuery`
- **Response:** `TableDataInfo<DocDocumentRecordVo>`

### `GET /docman/document/{id}`

- **Permission:** `docman:document:query`
- **Purpose:** 查询文档详情
- **Path:** `id`
- **Response:** `R<DocDocumentRecordVo>`

### `GET /docman/document/{id}/download`

- **Permission:** `docman:document:download`
- **Purpose:** 下载文档原始内容
- **Path:** `id`
- **Response:** 文件流

### `POST /docman/document/{id}/viewer-ticket`

- **Permission:** `docman:document:query`
- **Purpose:** 创建在线预览票据
- **Path:** `id`
- **Response:** `R<DocViewerTicketVo>`

### `GET /docman/document/{id}/viewer-url`

- **Permission:** `docman:document:query`
- **Purpose:** 生成拼装后的在线预览地址
- **Path:** `id`
- **Response:** `R<DocViewerUrlVo>`

### `GET /docman/document/viewer/content/{ticket}`

- **Permission:** `@SaIgnore`
- **Purpose:** 通过票据读取预览内容
- **Path:** `ticket`
- **Response:** 文件流（inline）
- **Notes:** 仍会校验 Redis 票据和项目关系

### `POST /docman/document/upload`

- **Permission:** `docman:document:upload`
- **Purpose:** 上传文档并登记记录
- **Consumes:** `multipart/form-data`
- **Body:** `file`
- **Query/Form:** `projectId`
- **Constraints:** 最大 100MB；允许 `pdf/doc/docx/xls/xlsx/ppt/pptx/txt/png/jpg/jpeg/zip/rar`
- **Response:** `R<Void>`

### `DELETE /docman/document/{id}`

- **Permission:** `docman:document:delete`
- **Purpose:** 逻辑删除文档记录 / 标记失效
- **Path:** `id`
- **Response:** `R<Void>`

## 5. 节点截止时间管理

**Base Path:** `/docman/node/deadline`

### `GET /docman/node/deadline/list`

- **Permission:** `docman:nodedeadline:query`
- **Purpose:** 查询项目下节点截止时间配置
- **Query:** `projectId`
- **Response:** `R<List<DocNodeDeadlineVo>>`

### `PUT /docman/node/deadline`

- **Permission:** `docman:nodedeadline:edit`
- **Purpose:** 更新节点截止时间记录
- **Body:** `DocNodeDeadlineBo`
- **Response:** `R<Void>`

### `GET /docman/node/deadline/nodes`

- **Permission:** `docman:nodedeadline:query`
- **Purpose:** 读取流程定义内节点及其持续天数
- **Query:** `definitionId`
- **Response:** `R<List<FlowNodeDurationVo>>`

### `PUT /docman/node/deadline/node-duration`

- **Permission:** `docman:nodedeadline:edit`
- **Purpose:** 修改流程节点扩展里的 `durationDays`
- **Body:** `NodeDurationBo`
- **Response:** `R<Void>`

## 6. 插件管理与执行日志

**Base Path:** `/docman/plugin`

### `GET /docman/plugin/list`

- **Permission:** `docman:plugin:list`
- **Purpose:** 查询已注册插件列表
- **Response:** `R<List<DocPluginInfoVo>>`

### `GET /docman/plugin/execution/list`

- **Permission:** `docman:plugin:list`
- **Purpose:** 分页查询插件执行日志列表
- **Query:** `projectId`, `processInstanceId?`, `nodeCode?`, `pluginId?`, `PageQuery`
- **Response:** `TableDataInfo<DocPluginExecutionLogVo>`

### `GET /docman/plugin/execution/{id}`

- **Permission:** `docman:plugin:list`
- **Purpose:** 查询单条插件执行日志详情
- **Path:** `id`
- **Response:** `R<DocPluginExecutionLogVo>`

### `POST /docman/plugin/execution/trigger`

- **Permission:** `docman:plugin:trigger`
- **Purpose:** 手动触发指定流程节点上的插件
- **Body:** `DocPluginTriggerBo`
- **Response:** `R<Void>`

## 7. 归档管理

**Base Path:** `/docman/archive`

### `POST /docman/archive/{projectId}`

- **Permission:** `docman:archive:execute`
- **Purpose:** 对项目执行归档
- **Path:** `projectId`
- **Response:** `R<DocArchivePackageVo>`

### `GET /docman/archive/{projectId}`

- **Permission:** `docman:archive:query`
- **Purpose:** 查询项目最新归档
- **Path:** `projectId`
- **Response:** `R<DocArchivePackageVo>`

### `GET /docman/archive/history/{projectId}`

- **Permission:** `docman:archive:query`
- **Purpose:** 查询项目归档历史
- **Path:** `projectId`
- **Response:** `R<List<DocArchivePackageVo>>`

### `GET /docman/archive/{archiveId}/download`

- **Permission:** `docman:archive:download`
- **Purpose:** 下载归档 ZIP
- **Path:** `archiveId`
- **Response:** 文件流

## 8. 看板聚合接口

**Base Path:** `/docman/dashboard`

### `GET /docman/dashboard/overview`

- **Permission:** `docman:project:list`
- **Purpose:** 查询看板总览
- **Response:** `R<DocDashboardOverviewVo>`

### `GET /docman/dashboard/todo-summary`

- **Permission:** `docman:dashboard:todo-summary`
- **Purpose:** 查询待办汇总
- **Response:** `R<DocTodoSummaryVo>`

### `GET /docman/dashboard/project-progress`

- **Permission:** `docman:project:list`
- **Purpose:** 查询项目进度列表
- **Response:** `R<List<DocProjectProgressVo>>`

### `GET /docman/dashboard/deadline-alert`

- **Permission:** `docman:project:list`
- **Purpose:** 查询节点时限预警列表
- **Response:** `R<List<DocDeadlineAlertVo>>`

### `GET /docman/dashboard/plugin-stats`

- **Permission:** `docman:project:list`
- **Purpose:** 查询插件执行统计
- **Response:** `R<List<DocPluginStatsVo>>`

## 权限矩阵摘要

- 项目域：`docman:project:*`
- 成员域：依附项目查询/编辑权限
- 流程域：`docman:process:*`
- 文档域：`docman:document:*`
- 归档域：`docman:archive:*`
- 插件域：`docman:plugin:*`
- 节点时限域：`docman:nodedeadline:*`
- 看板域：`docman:dashboard:todo-summary` 与复用 `docman:project:list`

## 主要返回对象

- 项目：`DocProjectVo`, `DocProjectMemberVo`, `DocProjectProgressVo`
- 流程：`DocProcessConfigVo`, `FlowNodeDurationVo`
- 文档：`DocDocumentRecordVo`, `DocViewerTicketVo`, `DocViewerUrlVo`
- 归档：`DocArchivePackageVo`
- 看板：`DocDashboardOverviewVo`, `DocDeadlineAlertVo`, `DocTodoSummaryVo`, `DocPluginStatsVo`
- 插件：`DocPluginInfoVo`, `DocPluginExecutionLogVo`

---

_Generated using BMAD Method `document-project` workflow_
