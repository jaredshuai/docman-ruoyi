---
project_name: 'docman-ruoyi'
user_name: 'Jared'
date: '2026-04-01T15:22:07+08:00'
sections_completed: ['technology_stack', 'language_rules', 'framework_rules', 'testing_rules', 'quality_rules', 'workflow_rules', 'anti_patterns']
status: 'complete'
existing_patterns_found: 9
rule_count: 72
optimized_for_llm: true
---

# Project Context for AI Agents

_This file contains critical rules and patterns that AI agents must follow when implementing code in this project. Focus on unobvious details that agents might otherwise miss._

---

## Technology Stack & Versions

- **Language:** Java 17 only. Do not introduce Java 21+ APIs, syntax, or dependencies that require a higher bytecode level.
- **Build:** Maven multi-module project (`ruoyi-vue-plus`, revision `5.5.3`).
- **Framework:** Spring Boot `3.5.9`; all new web/framework code must stay on the Spring Boot 3 / `jakarta.*` namespace model. Do not mix in `javax.*`.
- **Persistence:** MyBatis `3.5.19`, MyBatis-Plus `3.5.16`; keep using the existing MyBatis-Plus patterns (`LambdaQueryWrapper`, `Wrappers.lambdaQuery()`, `PageQuery`, `TableDataInfo`, logic-delete conventions). Do not introduce a second DAO / ORM style.
- **Workflow:** Warm-Flow `1.8.4`; workflow integration must stay compatible with the current node extension contract, especially `plugins` and `archiveFolderName` fields parsed by `DocWorkflowNodeApplicationService`.
- **Cache / Runtime Infra:** Redis + Redisson `3.52.0`; Redis is not optional in this module because it is part of project-access caching and viewer-ticket flows. Preserve key compatibility, TTL behavior, and invalidation logic.
- **Storage:** AWS SDK v2 `2.28.22` behind `DocumentStoragePort`; keep OSS/object-storage logic compatible with the existing local-file fallback path for dev and degraded environments.
- **Docs / Mapping:** Therapi Runtime Javadoc `0.15.0`, MapStruct-Plus `1.5.0`; when adding public entry points or key orchestration methods, prefer function-level Javadoc so runtime documentation tooling keeps working.
- **Testing Baseline:** Maven Surefire `3.5.3`, Spring Boot Test, JUnit 5, Mockito Inline `5.2.0`; do not introduce JUnit 4, TestNG, PowerMock, or any plugin/config that changes the current test discovery model.
- **Layering Constraint:** Keep the current command/query split: controller for boundary + permission, `application/service` for orchestration, `service/impl` for transaction/aggregate logic. Do not collapse business orchestration back into controllers.
- **Regression Priority for version-sensitive changes:** if touching Spring Boot, MyBatis-Plus, Warm-Flow, Redis, or storage SDK behavior, always re-check three paths first: permission chain, workflow-node plugin chain, and upload/preview/archive chain.
- **Minimum compatibility gate:** changes affecting controller / application / service / plugin / workflow paths must at least keep `mvn -pl ruoyi-modules/ruoyi-docman -am -DskipTests compile` green, and should prefer adding same-layer tests rather than relying only on manual verification.

## Critical Implementation Rules

### 语言规则

- 只使用 **Java 17** 能力实现，不要引入预览特性、Java 21+ API、语法糖或要求更高字节码版本的依赖。
- 在 Spring Boot 3 代码中统一使用 **`jakarta.*`** 命名空间，尤其是 servlet、validation、web 相关注解；不要混入 `javax.*`。
- 新代码优先使用 **构造器注入**，保持 `final` 字段 + `@RequiredArgsConstructor` 风格，避免字段注入。
- 面向业务的校验失败、权限失败或用户可感知错误，优先沿用现有 **`ServiceException`** 模式，不要把通用运行时异常直接抛到控制层。
- 公开入口方法和关键编排方法优先补 **简洁的函数级 Javadoc**，因为仓库启用了 Therapi Runtime Javadoc 编译链。
- 遵循现有命名模式：`DocXxxController`、`DocXxxApplicationService`、`IDocXxxService`、`DocXxxServiceImpl`、`DocXxxBo`、`DocXxxVo`、`DocXxx` 实体。
- 对空集合或简单不可变结果，优先沿用当前代码风格，例如 `List.of()`、Stream 的 `.toList()`，不要混入风格不一致的旧写法。
- 持久层相关 Java 写法继续沿用当前 **MyBatis-Plus 3.5.x** 模式，例如 `LambdaQueryWrapper`、`Wrappers.lambdaQuery()`、`PageQuery`、`TableDataInfo`，不要在 Java 代码里引入另一套 DAO/ORM 风格。

### 框架规则

- 保持当前 **Spring Boot 分层边界**：controller 只处理参数绑定、权限注解和响应包装；`application/service` 负责跨服务编排；`service/impl` 负责事务、聚合逻辑和 mapper 协调。不要把复杂业务重新塞回 controller。
- **项目级权限** 统一通过 `DocProjectAccessServiceImpl` + `DocProjectPermissionPolicy` 解析和断言；不要在 controller、application service 或其他普通 service 中重复散写角色判断。
- **Warm-Flow 集成边界** 必须固定：流程启动走 `ProcessEnginePort`，流程节点完成事件走 `DocmanNodeListener -> DocWorkflowNodeApplicationService`，不要在其他层直接散点调用 `FlowEngine` 或绕开监听器编排链路。
- **节点扩展 `ext` 结构** 必须兼容现有约定，尤其是 `plugins`、`archiveFolderName` 及相关 JSON 结构；修改结构时必须同步更新解析、插件触发和回归测试，否则会直接破坏节点执行链路。
- **文档存储** 统一经过 `DocumentStoragePort`；业务层不要直接调用 AWS SDK、本地文件 API 或自己拼存储路径。无论如何都必须保留 OSS 失败时的本地降级路径。
- **在线预览链路** 必须继续沿用 `DocDocumentViewerApplicationService` 的 Redis 票据模式；即使 `viewer/content` 使用 `@SaIgnore`，也不能跳过票据有效期、项目归属和内容读取校验，更不能暴露真实 NAS 路径给外部。
- **插件执行** 统一经过 `PluginRegistry` + `PluginExecutor` + `DocWorkflowNodeApplicationService`；不要直接调用插件实现类，否则会丢失执行日志、请求快照、结果快照和产物登记。
- **定时任务与异步补偿逻辑** 继续放在 `job` / `listener` 入口，不要把提醒、补偿或批处理逻辑塞进 controller、query service 或普通接口请求链路。
- 只要改动涉及 **Spring Boot 分层、Warm-Flow、Redis、对象存储、插件运行时**，优先回归三条链路：权限链路、节点插件链路、上传/预览/归档链路。
- 如果新增框架集成点，优先复用现有 **port/adapter** 模式；不要直接把第三方 SDK 耦合进 controller、domain service 或通用业务服务。

### 测试规则

- 保持当前 **JUnit 5 + Spring Boot Test + Mockito Inline** 测试基线，不要引入 JUnit 4、TestNG、PowerMock 或会破坏现有测试发现机制的新框架。
- 保持现有测试分层不变：新增测试优先放到 `controller / application / service / domain / infrastructure / job / listener / plugin / sql` 对应目录，不要另起新的测试组织方式。
- **按风险选测试层级，优先低层验证**：能用单元/服务层测试覆盖的逻辑，不要默认推到 controller 或更外层；只有涉及权限注解、请求绑定、响应包装时才优先 controller 测试。
- **改哪一层，先补哪一层的测试**：改了 `controller` 先补 controller 测试，改了 `application/service` 先补应用服务测试，改了 `service/impl` 先补服务实现测试；不要总想着只补最外层接口测试。
- 明确各层测试边界：`controller` 测接口边界与错误响应，`application/service` 测编排，`service/impl` 测事务与聚合逻辑，`plugin/workflow` 测节点触发与执行链路，避免一条测试跨太多层导致定位困难。
- 对 `application/service`、`plugin/workflow` 这类编排代码，测试重点应放在 **调用顺序、分支路径、异常处理和协作者调用是否正确**，而不是只看最终返回值。
- 只要涉及 **权限判断、Warm-Flow 节点触发、插件执行、Redis 预览票据、上传/预览/归档链路**，就不要只靠手工联调，至少补最靠近变更点的自动化回归测试。
- SQL、Mapper XML、实体字段、SQL 脚本或表结构发生变化时，优先检查并补 `sql` / `mapper` 契约测试，避免“库表、XML、Java 字段”三者失配。
- 测试命名继续贴现有风格，沿用 `XxxTest`、`XxxContractTest` 这类命名方式，不要混入新的命名体系。
- 最低兼容性门槛是保持 `mvn -pl ruoyi-modules/ruoyi-docman -am -DskipTests compile` 通过；如果改的是核心业务链路，应至少补同层测试，最好执行对应模块测试，而不是只做人工验证。

### 代码质量与风格规则

- 保持当前代码组织偏向 **单一职责 + 清晰分层**；一个类只承担一个主职责，不要把接口边界、应用编排、事务聚合、外部适配、任务调度、监听响应混写在同一个类里。
- **controller 不承载复杂业务编排**；跨服务流程、节点触发、归档协调、插件调度应继续下沉到 `application/service` 或 `service/impl`。
- **禁止跨层直连底层依赖**：业务代码不要跳过既有边界直接访问 mapper、Redis、对象存储 SDK、Warm-Flow 原生 API；优先走现有 `service / port / adapter / application service` 链路。
- 新增公开入口、关键编排方法、状态流转方法和隐含约定较强的逻辑时，优先补 **简洁函数级 Javadoc**；注释解释“为什么这样做 / 约束是什么”，不要只复述代码表面含义。
- 错误处理优先延续现有 **`ServiceException` + 明确错误信息** 模式；不要把底层 SDK、IO、框架异常原样泄漏到接口边界。
- 持久层 Java 写法继续保持 **MyBatis-Plus Wrapper** 风格一致；不要在同一模块内混入另一套 DAO/Repository/ORM 风格。
- 命名必须继续贴职责语义：`Controller / ApplicationService / ServiceImpl / Mapper / Bo / Vo / Entity / Adapter / Job / Listener / Plugin`，让类名本身能暴露所在层级。
- 任何新的 **状态值、缓存 key、节点 ext 字段、存储路径约定、插件配置字段**，都要在代码中以常量、枚举、封装方法或注释形式显式表达，不能只靠隐含约定传播。
- 对已有模式的扩展优先“贴现有写法”，不要为了局部优化引入风格孤岛，否则后续 AI 和人工都会误判这是不是新的标准模式。
- 像流程节点完成、插件调度、归档协调、预览票据处理这类 **跨层协调逻辑必须继续收敛到少数中心入口**，不要在多个类里复制近似流程。

### 开发工作流规则

- 只要任务涉及 **deploy、release、SSH、MySQL、Redis、SnailJob 或其他环境协同**，必须先读取 `AGENTS.local.md`；不要跳过这一步做环境假设。
- **禁止** 把 `AGENTS.local.md` 中的敏感内容复制到受版本控制文件，也不要提交 `AGENTS.local.md` 本身。
- 后端自动部署分支是 **`dev`** 和 **`release`**；**不要默认 `main` 会自动部署**，也不要把“合到 main 等于发布”当成工作流前提。
- brownfield 改动前，优先阅读当前项目上下文：`docs/index.md`、`docs/architecture.md`、`docs/api-contracts.md`、`docs/data-models.md`、`_bmad-output/project-context.md`；不要脱离现有文档直接开改。
- 先判断当前任务属于 **文档 / 规划 / 实现 / 验证 / 发布** 中哪一类，再执行对应动作；不要把 PRD、架构、实现、测试、发布混成一个模糊步骤。
- 涉及 **SQL 脚本、实体、Mapper XML、服务层、接口层** 的联动改动时，优先按“先契约/结构，再实现，再测试，再发布确认”的顺序推进，避免上下游脱节。
- 提交和推送时只包含与当前任务直接相关的文件；**不要顺手带上** 无关删除、临时目录、agent 目录、_bmad 安装文件或本地生成物。
- 仓库工作树不干净时，默认采用 **精确择件提交**；不要因为一次小改动顺手清理或提交其他未明确授权的改动。
- 高风险改动优先采用“**先局部编译/局部验证，再扩大范围**”的节奏，不要一次性跨多层大改后再整体排错。
- 如果改动影响环境行为、部署行为或运行时依赖，先把关键假设写进文档、注释或提交说明，再推进实现，避免把隐式运维假设埋进代码。
- 如果改动更新了共享约定、接口契约或工程流程，相关 `docs/` 或 `project-context.md` 应同步更新，避免文档与实现长期漂移。

### 关键勿漏规则

- **不要绕过项目权限链路。** 项目级动作必须继续通过 `DocProjectAccessServiceImpl` + `DocProjectPermissionPolicy` 校验，不能在别的层复制或自造权限判断。
- **不要直接手写状态字符串绕过状态机。** 项目、流程、文档、归档相关状态流转应优先走现有枚举、状态机或封装方法，不要在业务代码里随意写新的状态值。
- **不要破坏节点 `ext` 结构兼容性。** `plugins`、`archiveFolderName` 等字段一旦改结构，就会直接影响 `DocWorkflowNodeApplicationService` 的解析、插件触发和回归测试。
- **不要把对象存储当成唯一存储路径。** 文档存储必须保留本地降级能力；开发环境和异常场景下不能假设 OSS 一定可用。
- **不要把 `viewer/content` 的 `@SaIgnore` 当成“无校验”。** 在线预览仍必须校验 Redis 票据、有效期和项目归属，不能泄漏真实 NAS 路径，也不能跳过内容读取校验。
- **不要直接调用插件实现类。** 插件执行必须经过 `PluginRegistry` / `PluginExecutor` / `DocWorkflowNodeApplicationService`，否则执行日志、请求快照、结果快照和产物登记会丢失。
- **不要让表结构、实体、Mapper XML 和 SQL 脚本脱节。** 只要改动其一，就要联动检查其他几层，并补 `sql` / `mapper` 契约测试。
- **不要默认 `main` 会自动部署。** 发布相关判断必须记住只有 `dev` / `release` 接入后端部署工作流。
- **不要在环境协同类任务中跳过 `AGENTS.local.md`。** 涉及 deploy、release、SSH、MySQL、Redis、SnailJob 或其他环境前置资料时，必须先读它，再决定动作。
- **不要把敏感环境知识写进受版本控制文件。** `AGENTS.local.md` 的内容不能复制到代码、文档或提交历史里，也不能把该文件本身提交。
- **不要把共享约定只留在聊天记录或提交说明里。** 影响后续协作的重要规则应沉淀到 `docs/` 或 `project-context.md`。

---

## Usage Guidelines

**For AI Agents:**

- 在实现任何 `docman` 相关改动前，先阅读本文件，再结合 `docs/index.md` 和对应专题文档工作。
- 遇到权限、状态流转、流程节点、插件执行、预览票据、上传/归档等高风险链路时，优先选择更保守、更贴现有模式的实现方式。
- 如果规则与代码现实冲突，先查证仓库事实来源，再更新文档或上下文，不要自行发明新规范。
- 当变更引入新的共享约定时，同步更新本文件或 `docs/`，不要让规则只停留在聊天和提交说明里。

**For Humans:**

- 保持这份文件精简，只记录 AI 最容易误判但又对正确性影响最大的规则。
- 技术栈、部署分支、流程节点约定、测试门槛或共享边界发生变化时，及时更新本文件。
- 如果某条规则已经变成显而易见的常识或被代码结构强约束，可以考虑删除，避免文档膨胀。

Last Updated: 2026-04-01
