# Docman API 契约文档

> 本文件是前后端协作的**唯一接口契约**，所有 AI 协作者必须严格遵守。
> 修改接口必须同步更新本文档。

## 通用约定

### 响应格式
```json
// 单条数据
{ "code": 200, "msg": "操作成功", "data": { ... } }

// 分页数据
{ "code": 200, "msg": "查询成功", "rows": [...], "total": 100 }
```

### 分页参数
| 参数 | 类型 | 说明 |
|------|------|------|
| pageNum | int | 页码，从1开始 |
| pageSize | int | 每页条数 |
| orderByColumn | string | 排序字段（可选） |
| isAsc | string | asc/desc（可选） |

### 权限标识前缀
所有 docman 权限以 `docman:` 开头，Sa-Token 校验。

---

## 1. 项目管理 `/docman/project`

### 1.1 项目列表（分页）
```
GET /docman/project/list
Permission: docman:project:list
```

**Query 参数（可选筛选）：**
| 参数 | 类型 | 说明 |
|------|------|------|
| name | string | 项目名称（模糊匹配） |
| customerType | string | 客户类型：telecom / social |
| businessType | string | 业务类型：pipeline / weak_current |
| documentCategory | string | 文档类别：telecom / internal / customer |
| status | string | 项目状态：active / archived |
| pageNum | int | 页码 |
| pageSize | int | 每页条数 |

**响应 `rows` 元素：**
```json
{
  "id": 1,
  "name": "XX通信管道工程",
  "customerType": "telecom",
  "businessType": "pipeline",
  "documentCategory": "telecom",
  "status": "active",
  "ownerId": 1,
  "ownerName": "张三",
  "nasBasePath": "/telecom/XX通信管道工程",
  "nasDirStatus": "created",
  "remark": "",
  "memberIds": [1, 2, 3],
  "currentUserRole": "owner",
  "createBy": 1,
  "createTime": "2026-03-17 00:00:00",
  "updateTime": "2026-03-17 00:00:00"
}
```

### 1.2 项目详情
```
GET /docman/project/{id}
Permission: docman:project:query
```
**响应 `data`：** 同上

### 1.3 新增项目
```
POST /docman/project
Permission: docman:project:add
Content-Type: application/json
```
**请求体：**
```json
{
  "name": "XX通信管道工程",
  "customerType": "telecom",
  "businessType": "pipeline",
  "documentCategory": "telecom",
  "ownerId": 1,
  "remark": "备注",
  "memberIds": [2, 3]
}
```
**响应 `data`：** `Long` 新项目ID

### 1.4 修改项目
```
PUT /docman/project
Permission: docman:project:edit
```
**请求体：** 同新增，额外包含 `id` 字段

### 1.5 删除项目
```
DELETE /docman/project/{ids}
Permission: docman:project:remove
```
`ids` 为逗号分隔的ID列表，如 `/docman/project/1,2,3`

---

## 2. 文档中心 `/docman/document`

### 2.1 文档列表（分页）
```
GET /docman/document/list
Permission: docman:document:list
```
**Query 参数：**
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| projectId | long | 是 | 所属项目ID |
| pageNum | int | 否 | 页码 |
| pageSize | int | 否 | 每页条数 |

**响应 `rows` 元素：**
```json
{
  "id": 1,
  "projectId": 1,
  "projectName": "XX通信管道工程",
  "nodeInstanceId": 100,
  "pluginId": "excel-fill",
  "sourceType": "plugin",
  "fileName": "施工记录表.xlsx",
  "nasPath": "/telecom/XX通信管道工程/施工记录表.xlsx",
  "ossId": 50,
  "status": "generated",
  "generatedAt": "2026-03-17 10:00:00",
  "archivedAt": null,
  "createTime": "2026-03-17 10:00:00"
}
```

### 2.2 文档详情
```
GET /docman/document/{id}
Permission: docman:document:query
```

### 2.3 手动上传文档
```
POST /docman/document/upload
Permission: docman:document:upload
```
**请求体：**
```json
{
  "projectId": 1,
  "sourceType": "upload",
  "fileName": "设计方案.pdf",
  "nasPath": "/telecom/XX项目/设计方案.pdf",
  "ossId": 51
}
```

---

## 3. 流程编排 `/docman/process`

### 3.1 绑定流程
```
POST /docman/process/bind?projectId=1&definitionId=100
Permission: docman:process:bind
```

### 3.2 启动流程
```
POST /docman/process/start/{projectId}
Permission: docman:process:start
```
**响应 `data`：** `Long` 流程实例ID

### 3.3 查询流程配置
```
GET /docman/process/{projectId}
Permission: docman:process:query
```
**响应 `data`：**
```json
{
  "id": 1,
  "projectId": 1,
  "definitionId": 100,
  "instanceId": 200,
  "status": "running",
  "createTime": "2026-03-17 00:00:00",
  "updateTime": "2026-03-17 00:00:00"
}
```

---

## 4. 归档管理 `/docman/archive`

### 4.1 执行归档
```
POST /docman/archive/{projectId}
Permission: docman:archive:execute
```
**响应 `data`：**
```json
{
  "id": 1,
  "projectId": 1,
  "archiveNo": "ARC-XX通信管道工程-20260317-001",
  "archiveVersion": 1,
  "nasArchivePath": "/telecom/XX通信管道工程/归档",
  "manifest": [
    { "fileName": "施工记录表.xlsx", "nasPath": "/telecom/XX项目/施工记录表.xlsx", "sourceType": "plugin", "generatedAt": "2026-03-17" }
  ],
  "snapshotChecksum": "sha256:abcdef...",
  "status": "completed",
  "requestedAt": "2026-03-17 12:00:00",
  "completedAt": "2026-03-17 12:01:00",
  "createTime": "2026-03-17 12:00:00",
  "updateTime": "2026-03-17 12:01:00"
}
```

### 4.2 查询最新归档
```
GET /docman/archive/{projectId}
Permission: docman:archive:query
```

### 4.3 归档历史列表
```
GET /docman/archive/history/{projectId}
Permission: docman:archive:query
```
**响应 `data`：** 同上结构的数组，按 `archiveVersion` 降序

---

## 5. 插件管理 `/docman/plugin`

### 5.1 已注册插件列表
```
GET /docman/plugin/list
Permission: docman:plugin:list
```
**响应 `data`：**
```json
[
  {
    "pluginId": "excel-fill",
    "pluginName": "Excel 模板填充",
    "pluginType": "excel_fill",
    "inputFields": [{ "name": "templatePath", "type": "string", "required": true, "description": "模板路径" }],
    "outputFields": [{ "name": "filePath", "type": "string", "required": true, "description": "生成文件路径" }]
  }
]
```

### 5.2 插件执行日志（分页）
```
GET /docman/plugin/execution/list
Permission: docman:plugin:list
```
**Query 参数：**
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| projectId | long | 是 | 项目ID |
| processInstanceId | long | 否 | 流程实例ID |
| nodeCode | string | 否 | 节点编码 |
| pluginId | string | 否 | 插件ID |
| pageNum | int | 否 | 页码 |
| pageSize | int | 否 | 每页条数 |

**响应 `rows` 元素：**
```json
{
  "id": 1,
  "projectId": 1,
  "processInstanceId": 200,
  "nodeCode": "node_review",
  "pluginId": "excel-fill",
  "pluginName": "Excel 模板填充",
  "status": "success",
  "costMs": 1200,
  "generatedFileCount": 1,
  "errorMessage": null,
  "requestSnapshot": "{ ... }",
  "resultSnapshot": "{ ... }",
  "createTime": "2026-03-17 10:00:00"
}
```

---

## 枚举值参考

### 项目状态 (project.status)
| code | 说明 |
|------|------|
| active | 进行中 |
| archived | 已归档 |

### 客户类型 (customerType)
| code | 说明 |
|------|------|
| telecom | 通信 |
| social | 社会 |

### 业务类型 (businessType)
| code | 说明 |
|------|------|
| pipeline | 管道 |
| weak_current | 弱电 |

### 文档类别 (documentCategory)
| code | 说明 |
|------|------|
| telecom | 电信文档 |
| internal | 内部文档 |
| customer | 客户文档 |

### 文档状态 (document.status)
| code | 说明 | 可流转到 |
|------|------|----------|
| pending | 待生成 | running, generated, failed, obsolete |
| running | 生成中 | generated, failed, obsolete |
| generated | 已生成 | archived, obsolete |
| failed | 生成失败 | pending, running, obsolete |
| archived | 已归档 | (终态) |
| obsolete | 已失效 | pending, running |

### 归档状态 (archive.status)
| code | 说明 |
|------|------|
| requested | 已申请 |
| generating | 归档中 |
| completed | 已完成 |
| failed | 失败 |

### 流程状态 (process.status)
| code | 说明 |
|------|------|
| pending | 待启动 |
| running | 运行中 |
| completed | 已完成 |

### 项目角色 (member.roleType)
| code | 说明 | 权限范围 |
|------|------|----------|
| owner | 负责人 | 全部操作 |
| editor | 编辑 | 查看/编辑项目、查看/上传文档、查看流程、查看归档 |
| viewer | 只读 | 查看项目、查看文档、查看流程、查看归档 |

### NAS 目录状态 (nasDirStatus)
| code | 说明 |
|------|------|
| pending | 待创建 |
| created | 已创建 |
| failed | 创建失败 |

### 文档来源类型 (sourceType)
| code | 说明 |
|------|------|
| plugin | 插件自动生成 |
| upload | 手动上传 |

### 插件执行状态 (execution.status)
| code | 说明 |
|------|------|
| success | 成功 |
| failed | 失败 |
