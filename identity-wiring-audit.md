# docman 接入授权中心：可信 Principal 最小接线审计（只读）

- 审计日期：2026-10-27
- 范围：仅以下 7 个文件的静态阅读；未运行构建/测试、未连接任何 live 环境、未修改应用代码
- 脱敏声明：本报告不含任何凭据值、token、密码、kubeconfig、JWT 样例或完整 secret URL

## 一、已存在事实（代码证据）

1. **身份载体是 LoginUser，主键是本地 userId**
   `LoginUser.java:33,103,138-146`：`userId/username/tenantId/userType` 均为本地字段；`getLoginId()` 返回 `userType + ":" + userId`。当前模型**没有** issuer / sub / external-principal 字段。

2. **登录上下文写入走 sa-token extra + 服务端 Session**
   `LoginHelper.java:50-61`：`login()` 把 tenantId/userId/userName/deptId 等作为 extra 编入 token，并把完整 `LoginUser` 存入 `StpUtil.getTokenSession()`。读取侧 `getUserId()/getUsername()` 均从 token extra 解析（`LoginHelper.java:90-113`）。

3. **token 为 sa-token JWT 简单模式 + 自定义 DAO（服务端会话仍在）**
   `SaTokenConfig.java:25-28,42-44`：`StpLogicJwtForSimple`（JWT 仅作 token 载体）+ `PlusSaTokenDao`。含义：JWT 被伪造后仍需服务端会话配合，且具备吊销通道——这是既有优势，不需要重造。

4. **请求级鉴权入口单一，且已有"header 与 token 绑定值比对"的防伪先例**
   `SecurityConfig.java:51-87`：`SaInterceptor` 对所有 URL `checkLogin()`，并强制 request header/param 中的 `clientid` 与 **token 内** 的 clientId 一致，不一致即判定 token 无效。这是现成的"不信任裸 Header、以 token/服务端会话为准"的模式。actuator 走独立的 Basic 鉴权（`SecurityConfig.java:93-106`，值取自配置，不落地到代码）。

5. **第三方身份 → 本地用户的绑定模式已存在**
   `SocialAuthStrategy.java:61-87`：外部授权返回 `AuthUser(source+uuid)`，经 `sysSocialService.selectByAuthId(...)` 查**预先绑定表**换取本地 `userId`，再 `buildLoginUser(user)`。未绑定即拒绝登录。这就是"外部 canonical principal 映射到本地用户"的既有、可复制的最小语义。

6. **docman 业务侧身份面收敛于 LoginHelper**
   `DocProjectController.java`：全部端点仅用 `@SaCheckPermission`，不读任何身份 Header。
   `DocProjectServiceImpl.java:51-52,61,69`：权限判定、可见项目过滤、成员缓存失效全部以 `LoginHelper.getUserId()`（本地 userId）为键。docman **不关心** token 从哪签发，只消费解析后的本地用户。

## 二、推荐的最小边界（不重写平台、不接受伪造 Header）

**原则：新增"签发侧"一条接缝，签发后与现有链路完全合流；docman 消费侧零改动。**

1. **在 IAuthStrategy 层新增一种授权中心登录策略（对标 SocialAuthStrategy）**
   - 职责：向授权中心**验证/交换**令牌 → 提取可信 `(issuer, sub)` → 查绑定表（复用 sys_social 式 authId 模式，键建议为 `issuer + sub` 组合，避免不同 issuer 的 sub 撞库）→ 得到本地 `SysUser` → `buildLoginUser` → `LoginHelper.login()`。
   - 验证必须由后端用授权中心公钥/introspection **独立校验**，不信任任何转发的身份声明。

2. **扩展 LoginUser，而不是复用 username/userId 承载外部身份**
   - 在 `LoginUser` 增加可选字段（如 `authIssuer`、`authPrincipalSub`），仅在授权中心登录路径填充。`issuer/sub ≠ userId/username`：后者是本地主键，保持为 docman 权限/数据归属键不变。

3. **把可信 principal 固化进服务端会话与 token extra**
   - 在 `LoginHelper.login()` 的 extras 中追加 issuer/sub（命名如 `authIssuer`、`authSub`），使后续请求可通过 `StpUtil.getExtra()` 读到——与现有 `CLIENT_KEY` 校验同构。

4. **防伪边界照抄 clientid 先例**
   - 若网关会转发身份 Header（如已验证主体头），拦截器只做"Header 与 token/会话内绑定值一致性"校验，**绝不**把 Header 作为身份来源；网关必须剥离外部同名 Header（见第三节前置）。
   - `SecurityConfig.java` 现有 excludes 清单不扩大。

5. **消费侧不动**
   - DocProjectController/ServiceImpl 继续只依赖 `@SaCheckPermission` + `LoginHelper.getUserId()`。这是本次审计确认的最大收益点：接线的爆炸半径仅在"登录/令牌验证"一条路径。

最小改动面估计：`LoginUser`（+2 字段）、`LoginHelper`（+2 常量与读取方法）、新增 1 个 AuthStrategy 实现 + 1 张绑定表、`SecurityConfig`（可选的 issuer/sub 一致性校验）。不改 docman 模块任何文件。

## 三、必须由 infra / 认证负责人提供的前置（代码侧无法自足）

1. **授权中心元数据**：issuer 标识、JWKS 端点或 introspection 端点、支持的 grant/令牌交换方式、docman 作为 client 的注册与 audience 约定。
2. **验签信任建立方式**：公钥分发/轮换机制，或 mTLS/网络隔离下走 introspection 的选型决定；以及后端是否可直接被绕过网关访问（若可，Header 剥离方案作废，必须走后端自验签）。
3. **网关 Header 策略**：外部请求中同名身份 Header 是否在入口被无条件剥离；内部转发头的命名与不可伪造路径。
4. **canonical principal 的定义**：`sub` 的格式、稳定性承诺（是否永不复用/回收）、是否需要 `issuer+sub` 命名空间化。
5. **绑定与开通流程**：`(issuer, sub) → 本地 userId` 的初始化数据谁来供（导入、审批、自助绑定），以及离职/禁用时绑定失效 SLA。
6. **会话与吊销语义对齐**：授权中心 token TTL 与 sa-token `timeout/activeTimeout`（`SocialAuthStrategy.java:94-95` 由 client 配置驱动）的映射；单点登出是否需回调吊销本地会话。
7. **租户映射**：授权中心侧是否有 tenant 概念，与 `tenantId` extra 的对应关系（多租户场景可参照 `SocialAuthStrategy.java:74-82` 的租户匹配拒绝模式）。

## 四、暂不实现（明确出界）

- **不自建 OIDC Provider**：不做授权服务端、不发自己的 id_token、不实现完整 OP 端点。
- **不把 docman 权限模型改为按外部 sub 授权**：`doc_project_member.user_id`、缓存键、数据权限继续用本地 userId。
- **不重写 LoginHelper 的全部调用方**：现有登录链路（密码/短信/social）不动，新策略只是并列新增。
- **不做细粒度 scope→permission 的自动映射**：菜单/角色权限仍由 RuoYi 现有 `StpInterface` 供给。
- **不动 actuator Basic 鉴权与 sse 排除路径**：与本次接线正交。
- **不引入工作负载身份（SPIFFE/mTLS 服务间认证）**：本期只解决"人的 principal"。

## 五、结论

docman 侧身份消费已经天然解耦（只见 `@SaCheckPermission` + `LoginHelper.getUserId()`），因此接入授权中心的最小边界只需一条"验证外部 token → 提取可信 (issuer,sub) → 查绑定表 → 走现有 `LoginHelper.login()` 合流"的签发侧接缝；`username/userId` 继续做本地主键、不承载 issuer/sub；防伪沿用 `SecurityConfig` 已有的 clientid 一致性校验先例并以服务端会话为准。阻塞项全部在第三节：issuer/JWKS、网关 Header 剥离承诺、(issuer,sub)→本地用户 的绑定数据来源与吊销语义，需 infra/认证负责人先给定，代码侧才有可验证的落地前提。
