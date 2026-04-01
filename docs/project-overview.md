# ruoyi-docman - Project Overview

**Date:** 2026-04-01  
**Type:** Backend  
**Architecture:** Layered backend module with workflow and plugin runtime

## Executive Summary

`ruoyi-docman` 是一个面向项目文档生产的后端业务模块，核心职责包括项目与成员管理、流程绑定与启动、节点上下文沉淀、插件驱动的文档产出、文档上传下载与在线预览、项目归档，以及看板/提醒能力。模块通过 Warm-Flow 驱动流程节点，通过插件体系生成或加工文档产物，并把产物和执行过程沉淀到 `doc_*` 业务表中。

## Project Classification

- **Repository Type:** Monolith（聚合仓中的单业务模块）
- **Project Type(s):** Backend
- **Primary Language(s):** Java 17
- **Architecture Pattern:** `controller -> application -> service/domain -> mapper/infrastructure -> plugin runtime`

## Technology Stack Summary

| Category | Technology | Notes |
| --- | --- | --- |
| Language | Java 17 | 根工程 `pom.xml` 指定 |
| Build | Maven | 通过聚合工程构建 |
| Framework | Spring Boot 3.5.9 | 宿主后端运行时 |
| Persistence | MyBatis-Plus + MyBatis XML | CRUD + 扩展查询 |
| Auth | Sa-Token | 权限注解与登录态 |
| Workflow | Warm-Flow 1.8.4 | 流程定义/实例/节点事件 |
| Cache | Redis | 角色缓存、预览票据 |
| Storage | OSS + 本地降级 | `DocumentStoragePort` |
| Plugin | 自定义 SPI + Runtime | AI 生成、数据抽取、Excel 填充 |
| File | EasyExcel | 归档清单生成 |
| Test | JUnit 5 + Mockito | 69 个测试文件 |

## Key Features

- 项目、成员、角色与可访问项目缓存管理
- 项目与 Warm-Flow 流程定义绑定、流程实例启动
- 节点上下文读写与跨节点字段聚合
- 插件注册、节点插件触发、执行日志与产物登记
- 文档上传、下载、在线预览票据与内容分发
- 项目归档、归档清单生成、归档历史下载
- 看板总览、进度、待办、超时预警与插件统计

## Architecture Highlights

- 控制器层职责克制，业务编排集中在 `application/service`
- 领域层存在明确的状态机与权限策略，而不是把规则散在 controller/service 中
- 插件体系与工作流引擎通过上下文对象和端口解耦
- 文档存储优先走对象存储，失败时降级到本地，适合开发环境和不完整环境
- 通过 `therapi-runtime-javadoc` 编译链，函数级注释可以被运行时元数据消费

## Development Overview

### Prerequisites

- JDK 17
- Maven 3.9+
- 数据库、Redis、Warm-Flow
- 宿主应用环境
- 可选 OSS/对象存储配置

### Getting Started

该模块通常在聚合工程中编译和测试，不单独运行。日常开发建议先在模块内编译/测试，再通过宿主后端验证 `/docman/**` 接口与流程链路。

### Key Commands

- **Install / Compile:** `mvn -pl ruoyi-modules/ruoyi-docman -am clean compile`
- **Dev Validation:** 通过宿主后端启动后访问 `/docman/**`
- **Build:** `mvn clean package`
- **Test:** `mvn -pl ruoyi-modules/ruoyi-docman -am -DskipTests=false test`

## Repository Structure

模块按职责拆分为：

- `controller`: API 边界
- `application/service`: 命令/查询与工作流编排
- `domain`: 实体、枚举、VO/BO、状态机、权限策略
- `service/impl`: 核心业务实现
- `plugin`: 插件协议、注册、执行运行时
- `infrastructure`: 外部系统适配器
- `mapper` + `resources/mapper/docman`: 数据访问
- `job` / `listener`: 定时任务与流程事件入口

## Documentation Map

For detailed information, see:

- [index.md](./index.md) - Master documentation index
- [architecture.md](./architecture.md) - Detailed architecture
- [source-tree-analysis.md](./source-tree-analysis.md) - Directory structure
- [development-guide.md](./development-guide.md) - Development workflow
- [api-contracts.md](./api-contracts.md) - REST APIs and permissions
- [data-models.md](./data-models.md) - Tables and relationships

---

_Generated using BMAD Method `document-project` workflow_
