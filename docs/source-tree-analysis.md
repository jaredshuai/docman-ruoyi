# ruoyi-docman - Source Tree Analysis

**Date:** 2026-04-01

## Overview

`ruoyi-docman` 是 `docman-ruoyi` 仓库中的单体后端业务模块，源码根位于 `ruoyi-modules/ruoyi-docman`。模块不提供独立 `main` 启动类，而是作为 `ruoyi-admin` 运行时装配的业务子模块存在；HTTP 入口位于 `controller` 包，异步/事件入口位于 `job` 与 `listener` 包，核心业务沿 `controller -> application -> service/domain -> mapper/infrastructure` 分层流转。

本次扫描统计到：

- `src/main/java` 下 `145` 个 Java 源文件
- `src/test/java` 下 `69` 个测试文件
- `src/main/resources/mapper/docman` 下 `8` 个 MyBatis XML

## Complete Directory Structure

```text
ruoyi-docman/
├── pom.xml
├── src/
│   ├── main/
│   │   ├── java/org/dromara/docman/
│   │   │   ├── application/
│   │   │   │   ├── assembler/
│   │   │   │   ├── port/out/
│   │   │   │   └── service/
│   │   │   ├── config/
│   │   │   ├── constant/
│   │   │   ├── context/
│   │   │   ├── controller/
│   │   │   ├── domain/
│   │   │   │   ├── bo/
│   │   │   │   ├── entity/
│   │   │   │   ├── enums/
│   │   │   │   ├── service/
│   │   │   │   └── vo/
│   │   │   ├── infrastructure/
│   │   │   │   ├── ai/
│   │   │   │   ├── knowledge/
│   │   │   │   ├── log/
│   │   │   │   ├── notify/
│   │   │   │   ├── storage/
│   │   │   │   └── workflow/
│   │   │   ├── job/
│   │   │   ├── knowledge/
│   │   │   ├── listener/
│   │   │   ├── mapper/
│   │   │   ├── plugin/
│   │   │   │   ├── annotation/
│   │   │   │   ├── impl/
│   │   │   │   └── runtime/
│   │   │   └── service/
│   │   │       └── impl/
│   │   └── resources/
│   │       └── mapper/docman/
│   └── test/java/org/dromara/docman/
│       ├── application/
│       ├── config/
│       ├── context/
│       ├── controller/
│       ├── domain/
│       ├── infrastructure/
│       ├── job/
│       ├── knowledge/
│       ├── listener/
│       ├── mapper/
│       ├── plugin/
│       ├── service/
│       └── sql/
└── target/
```

## Critical Directories

### `src/main/java/org/dromara/docman/controller`

模块的 HTTP API 入口层。

**Purpose:** 承接文档项目、项目成员、流程绑定/启动、文档上传下载/预览、归档、看板、插件执行和节点时限配置等请求。  
**Contains:** `DocProjectController`、`DocProjectMemberController`、`DocProcessController`、`DocDocumentRecordController`、`DocArchiveController`、`DocDashboardController`、`DocPluginController`、`DocNodeDeadlineController`。  
**Entry Points:** `/docman/project`、`/docman/project/{projectId}/member`、`/docman/process`、`/docman/document`、`/docman/archive`、`/docman/dashboard`、`/docman/plugin`、`/docman/node/deadline`

### `src/main/java/org/dromara/docman/application/service`

模块的应用服务层。

**Purpose:** 编排命令/查询场景，把控制器请求路由到服务接口、Assembler 和外部端口。  
**Contains:** 项目、成员、流程、文档、归档、节点时限、插件、工作流节点等应用服务。  
**Entry Points:** `DocWorkflowNodeApplicationService` 是工作流监听后的核心协调器，负责插件触发、上下文写入和产物登记。

### `src/main/java/org/dromara/docman/domain`

业务模型和状态规则层。

**Purpose:** 定义实体、VO/BO、枚举、权限策略、状态机以及节点上下文模型。  
**Contains:** 8 个持久化实体、9 个枚举、6 个领域服务。  
**Integration:** 该层被 `service/impl` 和 `application/service` 共同依赖，用于归档状态转换、流程状态转换、路径解析和权限决策。

### `src/main/java/org/dromara/docman/service/impl`

传统业务服务实现层。

**Purpose:** 封装 MyBatis-Plus 查询、事务边界、权限校验、归档与上传下载逻辑。  
**Contains:** 项目、成员、流程、访问控制、文档记录、提醒、归档、节点上下文、看板等实现类。  
**Integration:** 直接依赖 `mapper`、`application/port/out` 和 `domain/service`。

### `src/main/java/org/dromara/docman/plugin`

文档插件 SPI 与运行时。

**Purpose:** 定义插件协议、插件上下文、插件注册中心和执行器，用于在 Warm-Flow 节点完成时执行 AI/抽取/填充类插件。  
**Contains:** `DocumentPlugin`、`PluginRegistry`、`PluginExecutor`、`PluginContext`、`PluginResult` 及 3 个内置插件实现。  
**Integration:** 由 `DocWorkflowNodeApplicationService` 调用，产出文件后回写 `doc_document_record` 和 `doc_plugin_execution_log`。

### `src/main/java/org/dromara/docman/infrastructure`

外部系统适配层。

**Purpose:** 对接 OSS、本地文件兜底、Warm-Flow、SSE、AI 生成、知识检索、插件执行日志落库等外部能力。  
**Contains:** `OssDocumentStorageAdapter`、`WarmFlowProcessEngineAdapter`、`SseSystemMessageAdapter`、`HttpLlmGenerateAdapter`、`HttpKnowledgeSearchAdapter`、`MybatisPluginExecutionLogAdapter`。  
**Integration:** 对上实现 `application/port/out`，对下连接框架与外部服务。

### `src/main/java/org/dromara/docman/job` 与 `listener`

异步与事件入口。

**Purpose:** 处理待补建 NAS 目录、文档提醒、节点截止日期提醒，以及 Warm-Flow 节点完成事件。  
**Contains:** `DocumentReminderJob`、`NasRetryJob`、`NodeDeadlineReminderJob`、`DocmanNodeListener`、`DocNodeDeadlineListener`。  
**Entry Points:** 定时任务调度、工作流事件回调。

### `src/main/resources/mapper/docman`

MyBatis XML 扩展查询层。

**Purpose:** 补充复杂 SQL，如“当前用户可访问项目”的 JOIN 分页查询。  
**Contains:** `DocProjectMapper.xml`、`DocDashboardMapper.xml` 等 8 个映射文件。  
**Integration:** 与 `mapper` 接口配套使用，为 service 层提供数据库访问能力。

## Entry Points

- **HTTP 入口:** `src/main/java/org/dromara/docman/controller/*.java`
- **工作流事件入口:** `src/main/java/org/dromara/docman/listener/DocmanNodeListener.java`
- **定时任务入口:** `src/main/java/org/dromara/docman/job/DocumentReminderJob.java`、`src/main/java/org/dromara/docman/job/NasRetryJob.java`、`src/main/java/org/dromara/docman/job/NodeDeadlineReminderJob.java`
- **插件执行入口:** `src/main/java/org/dromara/docman/application/service/DocWorkflowNodeApplicationService.java`

## File Organization Patterns

- 控制器只保留权限注解、请求绑定和少量参数防御，业务编排下沉到 `application/service`
- `application/service` 区分命令类和查询类，查询尽量直接透传到 `service` / `assembler`
- `service/impl` 承担事务边界、缓存失效、权限断言、归档/下载/提醒等核心业务
- `domain/service` 聚焦不可变规则，如状态机、路径解析和角色权限矩阵
- `plugin` 与 `infrastructure` 明确实现 SPI + Port/Adapter 结构，避免业务层直接耦合第三方
- `mapper + mapper xml` 用于实体 CRUD 和少量手写 SQL

## Key File Types

### Controller

- **Pattern:** `controller/*Controller.java`
- **Purpose:** 暴露 REST API 与权限边界
- **Examples:** `DocProjectController.java`, `DocDocumentRecordController.java`

### Application Service

- **Pattern:** `application/service/*ApplicationService.java`
- **Purpose:** 编排命令、查询、工作流事件和插件触发
- **Examples:** `DocProjectApplicationService.java`, `DocWorkflowNodeApplicationService.java`

### Entity

- **Pattern:** `domain/entity/*.java`
- **Purpose:** 映射 `doc_*` 业务表
- **Examples:** `DocProject.java`, `DocDocumentRecord.java`, `DocArchivePackage.java`

### Mapper XML

- **Pattern:** `src/main/resources/mapper/docman/*.xml`
- **Purpose:** 扩展复杂 SQL 与分页查询
- **Examples:** `DocProjectMapper.xml`, `DocDashboardMapper.xml`

### Plugin

- **Pattern:** `plugin/**/*.java`
- **Purpose:** 定义和执行节点插件
- **Examples:** `PluginRegistry.java`, `PluginExecutor.java`, `AiGeneratePlugin.java`

### Test

- **Pattern:** `src/test/java/**/*.java`
- **Purpose:** 覆盖 controller、application、service、domain、infrastructure、job、listener、plugin 和 SQL 契约
- **Examples:** `DocProjectControllerTest.java`, `DocDocumentApplicationServiceTest.java`, `RyDocmanSqlContractTest.java`

## Asset Locations

该模块无前端静态资源目录，主要资源为 MyBatis XML。

## Configuration Files

- `pom.xml`: 模块依赖声明，接入 `ruoyi-common-*`、`ruoyi-system`、`ruoyi-workflow` 与 `warm-flow`
- `src/main/java/org/dromara/docman/config/DocmanAiConfig.java`: AI 插件相关配置
- `src/main/java/org/dromara/docman/config/DocmanViewerConfig.java`: 文档在线预览地址、启用开关、票据 TTL
- `src/main/java/org/dromara/docman/config/DocmanJobConfig.java`: 任务调度相关配置

## Notes for Development

- 该模块需要依附宿主应用启动，单独编译/测试可在聚合工程中使用 `-pl ruoyi-modules/ruoyi-docman -am`
- 文档存储优先通过 `DocumentStoragePort` 走 OSS/对象存储，失败时降级到本地 `docman.upload.localRoot`
- 工作流节点插件链依赖 Warm-Flow 节点扩展字段 `plugins`、`archiveFolderName` 等结构化配置
- 由于根工程启用了 `therapi-runtime-javadoc` 注解处理器，补充函数级注释可以被运行时工具链消费

---

_Generated using BMAD Method `document-project` workflow_
