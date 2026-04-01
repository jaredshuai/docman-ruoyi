# ruoyi-docman - Development Guide

**Date:** 2026-04-01

## 模块定位

`ruoyi-docman` 是聚合工程 `docman-ruoyi` 中的业务子模块，通常通过父工程或 `ruoyi-admin` 一起构建运行。开发时既可以单独编译/测试模块，也可以联动宿主应用做端到端验证。

## Prerequisites

- JDK 17
- Maven 3.9+
- 可用的数据库环境（模块依赖 MyBatis-Plus 持久化）
- Redis（预览票据、缓存、部分运行时依赖）
- Warm-Flow 运行环境与流程定义
- 可选的对象存储/OSS；未接通时支持本地文件降级

## 关键依赖

- Spring Boot `3.5.9`
- Java `17`
- MyBatis-Plus `3.5.16`
- Sa-Token `1.44.0`
- Warm-Flow `1.8.4`
- Hutool `5.8.43`
- EasyExcel `1.3.0`
- JUnit 5 + Mockito（测试）
- Therapi Runtime Javadoc（编译期生成运行时可读注释元数据）

## 常用命令

### 编译模块

```bash
mvn -pl ruoyi-modules/ruoyi-docman -am clean compile
```

### 运行模块测试

```bash
mvn -pl ruoyi-modules/ruoyi-docman -am -DskipTests=false test
```

### 仅运行指定测试

```bash
mvn -pl ruoyi-modules/ruoyi-docman -am -DskipTests=false -Dtest=DocProjectControllerTest test
```

### 构建整个后端

```bash
mvn clean package
```

## 本地开发建议

### 1. 先验证宿主应用能装载模块

该模块没有独立 `main` 方法。对 HTTP 接口、权限、流程启动和对象存储路径的验证，通常需要通过宿主后端启动后访问 `/docman/**` 路由完成。

### 2. 优先从以下入口排查问题

- **HTTP 请求问题:** `controller/*Controller.java`
- **业务编排问题:** `application/service/*ApplicationService.java`
- **数据/事务问题:** `service/impl/*ServiceImpl.java`
- **流程节点/插件问题:** `DocWorkflowNodeApplicationService`, `PluginExecutor`, `PluginRegistry`
- **SQL 问题:** `mapper/*.java` + `resources/mapper/docman/*.xml`
- **定时任务/提醒问题:** `job/*`

### 3. 配置相关关注点

- `DocmanViewerConfig`: 在线预览开关、viewer base URL、票据 TTL
- `DocmanAiConfig`: AI 生成插件相关 HTTP 配置
- `DocmanJobConfig`: 定时任务调度
- `docman.upload.localRoot`: 对象存储失败时的本地降级目录

## 目录到职责的映射

- `controller`: REST API 边界
- `application/service`: 命令/查询编排
- `domain/service`: 状态机、权限策略、路径规则
- `service/impl`: 事务、CRUD、缓存、归档、提醒
- `plugin`: 插件 SPI 与运行时
- `infrastructure`: 外部适配器
- `mapper` + `mapper/docman/*.xml`: 数据访问
- `test`: 模块级单元/集成契约测试

## 典型开发任务

### 新增一个项目域接口

1. 在 `controller` 增加路由
2. 在 `application/service` 增加命令或查询编排
3. 在 `service` / `service/impl` 补服务接口与实现
4. 需要状态流转时补 `domain/service` 规则
5. 补 controller / application / service 测试

### 新增一个流程节点插件

1. 实现 `DocumentPlugin`
2. 在类上使用 `@DocPlugin`
3. 根据需要补 `PluginContext` 字段读取
4. 让节点扩展字段 `plugins` 引用该插件 ID
5. 为插件实现、执行器和工作流编排补测试

### 扩展文档预览/存储能力

1. 优先修改 `DocumentStoragePort` 对应适配器
2. 保留本地降级能力，避免开发环境完全依赖 OSS
3. 验证 `DocDocumentApplicationService` 和 `DocDocumentViewerApplicationService`

### 调整归档规则

1. 先看 `DocArchiveServiceImpl`
2. 再看 `DocArchiveDomainService`
3. 最后检查 `DocDocumentStateMachine` 与 `DocProjectStateMachine`

## 测试覆盖概况

- `application/service`: 15 个测试
- `controller`: 9 个测试
- `service/impl`: 12 个测试
- `domain/service`: 6 个测试
- `job`, `listener`, `plugin`, `infrastructure`, `sql` 均有对应测试

这意味着做改动时，优先补同层测试通常就能接入现有验证方式。

## 开发中的注意事项

- 不要绕过 `DocProjectAccessServiceImpl` 直接做项目权限判断
- 不要在控制器层直接拼接复杂业务逻辑，应下沉到应用层或服务层
- 插件执行必须经过 `PluginExecutor`，这样日志和快照才会完整
- 预览接口即使使用 `@SaIgnore`，也必须保留票据校验逻辑
- 归档逻辑会批量更新文档和项目状态，修改时要保持事务边界

## 推荐排障顺序

1. 看 controller 是否正确绑定参数与权限
2. 看 application service 是否编排到了正确的 service / port
3. 看 service/impl 是否有状态机、缓存、事务副作用
4. 看 mapper XML / SQL 是否与实体字段一致
5. 看测试里是否已经覆盖相似场景

---

_Generated using BMAD Method `document-project` workflow_
