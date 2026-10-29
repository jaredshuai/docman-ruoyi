# docman Casdoor OIDC 登录回路与最小授权 PEP 交付

- 状态：**代码与离线测试已完成；未连接任何 live 环境**
- 隔离 worktree：`C:\Users\jared\AppData\Local\Temp\docman-java-consumer-0.1.1-20261027`
- 基线提交：`e0814ab39 fix(ci): 为 phase1 新表补 tenant_id 增量迁移`
- 说明：本文件取代此前“BLOCKED（Principal 身份来源缺口）”的审计结论。缺口已通过
  canonical issuer/sub 会话载体、标准 OIDC ID Token 验证器和只读绑定查询解决。

## 1. 本次实现范围

- Casdoor authorization-code + PKCE（S256）登录回路：
  - `GET /auth/oidc/login/start`
  - `GET /auth/oidc/callback`
  - `POST /auth/oidc/logout`（仅退出本地会话）
- 一次性 state/nonce/PKCE verifier 只保存在 Redis 服务端临时存储（TTL 默认 5 分钟，30 秒–10 分钟范围内归一化）。
- ID Token 使用既有 `CasdoorOidcTokenVerifier`（Spring Security JOSE）验签并校验 issuer、时间、audience、azp 与 nonce；
  `aud` 含多个值时要求 `azp` 显式等于本应用 client_id，`azp` 不匹配一律拒绝。
- 仅信任验证器输出的 canonical `(issuer, sub)`，通过既有只读绑定服务查找唯一、启用、租户匹配的本地用户，再调用既有 `LoginHelper.login()` 建立本地 Sa-Token 会话。
- 最小 PEP：`GET /docman/project/{id}` 读取前调用 `AuthorizationClient.check()`，`resourceType=project`，`resourceId=projectId`，
  attributes 只发送项目负责人的 canonical `ownerSubject`（见第 8 节），不再发送本地 userId、成员、角色、状态等未冻结属性。
- 启动安全检查：非 local 环境拒绝 FIXTURE/LOCAL 授权配置、未完成的 OIDC 配置和合成身份端点；prod 额外拒绝回环身份端点。

## 2. 改动文件

### 2.1 生产代码

| 文件 | 改动 |
| --- | --- |
| `ruoyi-common/.../core/domain/model/LoginUser.java` | 增加 canonical issuer/sub 字段（既有改动，本次复用） |
| `ruoyi-common/.../satoken/utils/LoginHelper.java` | 登录时写入/读取 canonical issuer/sub extra（既有改动，本次复用） |
| `ruoyi-admin/.../identity/oidc/DocmanOidcProperties.java` | 既有显式 OIDC 配置合同 |
| `ruoyi-admin/.../identity/oidc/CasdoorOidcTokenVerifier.java` | 既有标准 ID Token 验证器，支持 nonce 校验 |
| `ruoyi-admin/.../identity/CanonicalIdentity.java` | 既有 canonical 身份模型与稳定 authId |
| `ruoyi-admin/.../identity/CanonicalIdentityBindingService.java` | 既有只读绑定查询（未新增写路径） |
| `ruoyi-admin/.../identity/oidc/login/Pkce.java` | 新增：state/nonce/code verifier 与 S256 challenge |
| `ruoyi-admin/.../identity/oidc/login/OidcLoginTransaction*.java` | 新增：登录事务模型与存储接口 |
| `ruoyi-admin/.../identity/oidc/login/RedisOidcLoginTransactionStore.java` | 新增：Redis 一次性 state 存储（原子 `getAndDelete`） |
| `ruoyi-admin/.../identity/oidc/login/DocmanOidcLoginProperties.java` | 新增：显式登录配置，无默认 secret/身份 |
| `ruoyi-admin/.../identity/oidc/login/OidcTokenEndpointClient.java` | 新增：token endpoint 接口 |
| `ruoyi-admin/.../identity/oidc/login/RestClientOidcTokenEndpointClient.java` | 新增：表单交换 code + verifier，失败统一 503 |
| `ruoyi-admin/.../identity/oidc/login/OidcSessionAuthenticator.java` | 新增：本地会话建立接口 |
| `ruoyi-admin/.../identity/oidc/login/SaTokenOidcSessionAuthenticator.java` | 新增：复用 `LoginHelper.login()` 与 Sa-Token |
| `ruoyi-admin/.../identity/oidc/login/OidcLoginService.java` | 新增：登录编排、一次性 state 消费、绑定校验、本地会话 |
| `ruoyi-admin/.../identity/oidc/login/OidcLoginResult.java` | 新增：登录结果 |
| `ruoyi-admin/.../identity/oidc/login/OidcLoginException.java` | 新增：401/403/503 受控异常 |
| `ruoyi-admin/.../identity/oidc/login/OidcLoginConfiguration.java` | 新增：显式装配（无 mock/默认 provider） |
| `ruoyi-admin/.../identity/IdentitySafetyGuard.java` | 新增：启动安全检查 |
| `ruoyi-admin/.../controller/OidcLoginController.java` | 新增：start/callback/logout 端点 |
| `ruoyi-admin/.../controller/OidcLoginExceptionHandler.java` | 新增：OIDC 统一错误边界 |
| `ruoyi-modules/ruoyi-docman/.../config/DocmanAuthorizationProperties.java` | 新增：授权客户端显式配置 |
| `ruoyi-modules/ruoyi-docman/.../infrastructure/authz/DocmanAuthorizationClientProvider.java` | 新增：授权客户端持有者，未配置不创建 |
| `ruoyi-modules/ruoyi-docman/.../infrastructure/authz/LoginHelperCanonicalPrincipalProvider.java` | 新增：仅从会话 extra 读取 canonical issuer/sub 与当前租户 |
| `ruoyi-modules/ruoyi-docman/.../application/authz/CanonicalPrincipal.java` | 新增：PEP 主体模型（不接受参数/Cookie/Header） |
| `ruoyi-modules/ruoyi-docman/.../application/authz/CanonicalPrincipalProvider.java` | 新增：主体提供者接口（含当前租户） |
| `ruoyi-modules/ruoyi-docman/.../application/authz/OwnerSubjectResolver.java` | 新增：负责人 canonical 身份反解接口 |
| `ruoyi-modules/ruoyi-docman/.../infrastructure/authz/SocialOwnerSubjectResolver.java` | 新增：基于 `sys_social.auth_id` 的只读 OIDC 反解，非唯一/非法一律 fail closed |
| `ruoyi-modules/ruoyi-docman/.../application/authz/DocProjectAuthorizationService.java` | 新增：项目详情最小 PEP，attributes 仅含 `ownerSubject` |
| `ruoyi-modules/ruoyi-docman/.../application/authz/DocProjectAuthorizationException.java` | 新增：PEP 受控异常 |
| `ruoyi-modules/ruoyi-docman/.../controller/DocmanAuthorizationExceptionHandler.java` | 新增：PEP 统一错误边界 |
| `ruoyi-modules/ruoyi-docman/.../controller/DocProjectController.java` | `getInfo` 读取后接入 `checkRead`，allowed 保持原返回 |

### 2.2 配置

| 文件 | 改动 |
| --- | --- |
| `ruoyi-admin/src/main/resources/application.yml` | 新增 `docman.oidc` 与 `docman.authorization` 环境变量占位配置，无 secret 字面值 |
| `ruoyi-admin/pom.xml` | 增加 `spring-security-oauth2-jose` 与测试依赖（既有改动） |
| `ruoyi-modules/ruoyi-docman/pom.xml` | 增加 `com.jxccs:jxccs-authz-client:0.1.1` 与测试依赖（既有改动） |

环境变量名（仅名称，不含值）：

- OIDC：`DOCMAN_OIDC_ENABLED`、`DOCMAN_OIDC_ISSUER`、`DOCMAN_OIDC_JWKS_URI`、`DOCMAN_OIDC_AUDIENCE`、
  `DOCMAN_OIDC_CLIENT_ID`、`DOCMAN_OIDC_CLIENT_SECRET`、`DOCMAN_OIDC_AUTHORIZATION_URI`、
  `DOCMAN_OIDC_TOKEN_URI`、`DOCMAN_OIDC_REDIRECT_URI`、`DOCMAN_OIDC_TENANT_ID`、`DOCMAN_OIDC_SCOPE`
- 授权：`DOCMAN_AUTHORIZATION_ENABLED`、`DOCMAN_AUTHORIZATION_MODE`、`DOCMAN_AUTHORIZATION_PROVIDER`、
  `DOCMAN_AUTHORIZATION_APPLICATION`、`DOCMAN_AUTHORIZATION_ENVIRONMENT`、`DOCMAN_AUTHORIZATION_BASE_URL`、
  `DOCMAN_AUTHORIZATION_FIXTURE_SCENARIO`、`DOCMAN_AUTHORIZATION_SERVICE_CREDENTIAL`

`DOCMAN_OIDC_CLIENT_SECRET` 与 `DOCMAN_AUTHORIZATION_SERVICE_CREDENTIAL` 只能来自运行时 Secret/环境变量，
未写入仓库或本文件。

### 2.3 测试

| 文件 | 覆盖 |
| --- | --- |
| `ruoyi-admin/.../oidc/login/PkceTest.java` | verifier 长度/字符集、RFC 7636 S256 向量、随机性 |
| `ruoyi-admin/.../oidc/OidcLoginServiceTest.java` | start URL/PKCE、state 缺失/未知/重放、provider 错误、token 交换失败、nonce/签名/issuer/audience/过期、未绑定/停用/租户不匹配、成功会话与 canonical 字段 |
| `ruoyi-admin/.../oidc/login/RestClientOidcTokenEndpointClientTest.java` | 表单字段、200/id_token 缺失/空、500、网络异常、未配置 |
| `ruoyi-admin/.../controller/OidcLoginControllerTest.java` | 302、200 JSON、401/403/503 映射、logout 仅本地 |
| `ruoyi-admin/.../identity/IdentitySafetyGuardTest.java` | local fixture 允许、dev/prod 合成 provider/模式拒绝、配置不完整、prod 回环拒绝 |
| `ruoyi-modules/ruoyi-docman/.../authz/DocProjectAuthorizationServiceTest.java` | 无主体 401、负责人绑定缺失/租户缺失/解析异常 403 且不触达客户端、无客户端 503、allow、EXPLICIT_DENY/SCOPE_MISMATCH/MISSING_GRANT 403、传输失败 503、wire 仅含 `ownerSubject` |
| `ruoyi-modules/ruoyi-docman/.../authz/SocialOwnerSubjectResolverTest.java` | 合法 OIDC 解析、缺失/多条/租户不匹配/非 OIDC/非法 base64/非法 UTF-8/userId 或用户名冒充 sub 全部 fail closed |
| `ruoyi-modules/ruoyi-docman/.../controller/DocProjectControllerTest.java` | 保留原读取结果；拒绝路径不调用任何写服务 |
| `ruoyi-modules/ruoyi-docman/.../controller/DocmanAuthorizationExceptionHandlerTest.java` | 401/403/503 响应映射 |

既有测试未删除；`CasdoorOidcTokenVerifierTest`（含多 audience 时 `azp` 必须匹配的用例）、
`CanonicalIdentityBindingServiceTest` 继续通过。

## 3. 验证命令与结果

运行环境：Temurin JDK `17.0.17`（显式 `JAVA_HOME`）、Maven `3.9.11`、离线模式 `-o`。

### 3.1 编译

```bash
mvn -q -o -pl ruoyi-admin -am -DskipTests compile
mvn -o -pl ruoyi-admin -am -DskipTests test-compile
```

结果：`BUILD SUCCESS`。

### 3.2 OIDC 窄测试

```bash
mvn -o -pl ruoyi-admin -am -DskipTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest='PkceTest,OidcLoginServiceTest,RestClientOidcTokenEndpointClientTest,OidcLoginControllerTest,IdentitySafetyGuardTest,CasdoorOidcTokenVerifierTest,CanonicalIdentityBindingServiceTest' \
  test
```

结果：`Tests run: 69, Failures: 0, Errors: 0, Skipped: 0`。

### 3.3 docman PEP 窄测试

```bash
mvn -o -pl ruoyi-modules/ruoyi-docman -am -DskipTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest='DocProjectAuthorizationServiceTest,SocialOwnerSubjectResolverTest,DocProjectControllerTest,DocmanAuthorizationExceptionHandlerTest' \
  test
```

结果：`Tests run: 42, Failures: 0, Errors: 0, Skipped: 0`（第 8 节窄修后的最新结果；旧版本的 19 个用例已被替换）。

### 3.4 全量测试

```bash
mvn -o -DskipTests=false test
mvn -o -pl ruoyi-admin -am -DskipTests=false -Dmaven.test.failure.ignore=true test
```

结果：

- `ruoyi-docman`：`Tests run: 520, Failures: 3, Errors: 4`（新增 12 个测试后总数由 508 增至 520，
  失败集合与基线完全一致，均为 pre-existing）。
- `ruoyi-admin`：`Tests run: 74, Failures: 0, Errors: 0, Skipped: 1`（跳过项为既有 `TagUnitTest`），
  `BUILD SUCCESS`。

pre-existing 失败（与本次变更无关）：

- `DocMapperXmlContractTest`：XML-backed mapper 集合不一致、`DocProjectAddRecordDetailMapper.xml` namespace 不匹配、
  `DocProjectMapper.xml` 含 `${ew.customSqlSegment}`。
- `DocDocumentReminderServiceImplTest`、`DocProjectAccessServiceImplTest`（2 个用例）：`RedisUtils` 无法初始化。
- `DocProjectServiceImplTest`：`page` 为 null 导致 NPE。

## 4. 未做事项

- 未连接真实 Casdoor、真实授权中心、真实数据库或业务 VM；未做 live/dev 联调。
- 未实现 provider logout / `end_session_endpoint`；`POST /auth/oidc/logout` 只清除本地会话，不声称 Casdoor 已退出。
- 未新增数据库迁移；state/nonce/verifier 复用 Redis，登录会话复用 Sa-Token。
- 未扩展上传、AI、工作流、归档等其他业务入口；仅项目详情只读 GET 接入 PEP。
- 未新增通用 SDK API，未修改 authorization-center、SmartQuote、bid-risk、FinanceHub。
- 未提交、未推送、未部署。

## 5. 部署前置条件

1. 通过环境变量/Secret 提供完整 OIDC 与授权客户端配置；任一项缺失时登录返回 503、项目详情 PEP 返回 503（fail closed）。
2. 授权客户端非 local 环境必须使用 HTTP provider + INTEGRATION/RELEASE 模式；FIXTURE/LOCAL 仅允许 local。
3. 绑定关系需已存在于 `sys_social.auth_id`（`oidc:iss=<base64url(issuer)>:sub=<base64url(sub)>`），且同一 issuer/sub 在目标租户内唯一。
4. 外部身份绑定的本地用户必须启用、租户与 `DOCMAN_OIDC_TENANT_ID` 一致。

## 6. 脱敏声明

本交付未记录任何密码、token、JWT secret、client secret、服务凭据、kubeconfig 或带凭据 URL；
未读取或复制 `AGENTS.local.md` 中的敏感值；未连接 Casdoor/k3s/业务 VM/真实授权中心；
未提交、未推送、未部署。

## 7. 2026-10-29 窄修：disabled 授权配置的合成配置校验

### 7.1 缺口

`IdentitySafetyGuard.verifyAuthorization(local)` 原先在 `authorization.enabled=false` 时立即返回，
非 local/prod 环境即使携带 `mode=LOCAL`、`provider=FIXTURE`、`fixtureScenario` 等合成配置也不会启动失败，
与“integration/release 发现 LOCAL/FIXTURE/mock/debug 配置必须启动失败”的合同不符。

### 7.2 实际修改

| 文件 | 改动 |
| --- | --- |
| `ruoyi-admin/.../identity/IdentitySafetyGuard.java` | 非 local 环境在检查 `enabled` 之前先执行合成授权配置扫描；新增 `SYNTHETIC_AUTHORIZATION_TOKENS`（`local/mock/fixture/synthetic/dummy/fake/debug`）与 `rejectSyntheticAuthorizationConfig` / `rejectSyntheticAuthorizationValue`；非空 `fixtureScenario` 一律拒绝。`enabled=false` 且无合成配置时行为不变；local 的显式 fixture 仍允许 |
| `ruoyi-admin/.../identity/IdentitySafetyGuardTest.java` | 新增 5 个用例：disabled + LOCAL mode、disabled + FIXTURE provider、disabled + fixtureScenario、disabled + 合成 environment 值均拒绝；disabled 且无合成字段在非 local 通过 |

未改动 OIDC 校验、授权 SDK、业务 PEP、live 配置或其他文件。

### 7.3 验证

运行环境：Temurin JDK `17.0.17`（显式 `JAVA_HOME`）、Maven `3.9.11`、离线模式 `-o`。

```bash
mvn -o -pl ruoyi-admin -am -DskipTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false -Dtest=IdentitySafetyGuardTest test
```

结果：`BUILD SUCCESS`，`IdentitySafetyGuardTest` `Tests run: 15, Failures: 0, Errors: 0, Skipped: 0`
（既有 10 个用例保留并通过，新增 5 个边界用例通过）。

未提交、未推送、未部署。


## 8. 2026-10-29 窄修：PEP 资源属性收敛为 canonical ownerSubject

### 8.1 缺口

`DocProjectAuthorizationService` 原先把本地 `ownerId`、`memberIds`、`currentUserRole`、`status`、
`projectTypeCode` 放进 `ResourceRef.attributes`。授权中心的 SELF/owner scope 只识别 canonical
`ownerSubject`：

```json
{"ownerSubject": {"issuer": "...", "sub": "..."}}
```

本地 userId 不是跨系统授权主体，未冻结的策略属性也可能被授权中心忽略或误解释。本次只承诺
canonical owner scope，不承诺成员协作 scope。

### 8.2 实际修改

| 文件 | 改动 |
| --- | --- |
| `ruoyi-modules/ruoyi-docman/.../application/authz/OwnerSubjectResolver.java` | 新增只读反解接口；任何不确定结果返回 empty |
| `ruoyi-modules/ruoyi-docman/.../infrastructure/authz/SocialOwnerSubjectResolver.java` | 新增：通过既有 `ISysSocialService.queryListByUserId(ownerId)` 读取绑定；仅接受 `oidc:iss=<base64url>:sub=<base64url>`；要求唯一绑定、租户匹配、严格 base64url + UTF-8 解码；非 OIDC/多条/解码失败/查询异常一律 empty |
| `ruoyi-modules/ruoyi-docman/.../application/authz/CanonicalPrincipalProvider.java` | 增加 `currentTenantId()`，用于限定负责人绑定的租户 |
| `ruoyi-modules/ruoyi-docman/.../infrastructure/authz/LoginHelperCanonicalPrincipalProvider.java` | 从既有 Sa-Token 会话 extra 读取租户；缺失返回 empty |
| `ruoyi-modules/ruoyi-docman/.../application/authz/DocProjectAuthorizationService.java` | 顺序改为：会话主体（401）→ 负责人 canonical 绑定（403）→ 客户端配置/调用（503）→ 决策（403）；attributes 只构造 `{"ownerSubject":{"issuer","sub"}}`，移除 `ownerId`/`memberIds`/`currentUserRole`/`status`/`projectTypeCode` |
| `ruoyi-modules/ruoyi-docman/.../application/authz/DocProjectAuthorizationServiceTest.java` | 更新为 12 个用例：wire 只含 ownerSubject、负责人缺失/租户缺失/解析异常 403 且不触达客户端、其余 401/403/503 语义保持 |
| `ruoyi-modules/ruoyi-docman/.../infrastructure/authz/SocialOwnerSubjectResolverTest.java` | 新增 19 个用例：合法 OIDC、Unicode、无/null 绑定、多条、重复、租户不匹配、非 OIDC、username/裸 userId 冒充、非法 base64、非法 UTF-8、缺 sub、重复 sub、空 issuer、空 authId、空参数、查询异常 |

未修改数据库、`ruoyi-admin` OIDC 回路、授权中心 SDK、SmartQuote 或其他模块。

### 8.3 状态语义

| 场景 | 结果 |
| --- | --- |
| 当前 session 无可信 canonical issuer/sub | 401，不查绑定、不调用授权中心 |
| 负责人无绑定、多条、租户不匹配、非 OIDC、解码失败或租户缺失 | 403，先于客户端调用，不发 allow 请求 |
| 授权中心未配置/传输/协议/凭据故障 | 503 |
| 授权中心返回 allow | 放行，保持原读取结果 |
| 显式 deny / scope mismatch / missing grant | 403 |

拒绝路径不写业务数据：本次仅改动只读 PEP 与只读绑定查询，未新增任何写调用；
`DocProjectControllerTest.shouldFailClosedWithoutAnyBusinessWriteWhenAuthorizationDenied` 继续通过。

### 8.4 验证

运行环境：Temurin JDK `17.0.17`（显式 `JAVA_HOME`）、Maven `3.9.11`、离线模式 `-o`。

```bash
mvn -o -pl ruoyi-modules/ruoyi-docman -am -DskipTests test-compile

mvn -o -pl ruoyi-modules/ruoyi-docman -am -DskipTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest='DocProjectAuthorizationServiceTest,SocialOwnerSubjectResolverTest,DocProjectControllerTest,DocmanAuthorizationExceptionHandlerTest' \
  test
```

结果：`BUILD SUCCESS`，`Tests run: 42, Failures: 0, Errors: 0, Skipped: 0`：

- `DocProjectAuthorizationServiceTest`：12
- `SocialOwnerSubjectResolverTest`：19
- `DocProjectControllerTest`：8
- `DocmanAuthorizationExceptionHandlerTest`：3

本次未重跑全量测试；第 3.4 节记录的全量失败集合为 pre-existing，与本次改动无关。

### 8.5 未做事项与 live gates

未做事项：

- 未承诺成员协作 scope，未发送 `memberIds` 或角色；成员授权需后续独立冻结合同。
- 未新增/修改数据库表或迁移；绑定复用既有 `sys_social.auth_id`。
- 未修改授权中心 SDK、`ruoyi-admin` OIDC 回路、SmartQuote 或其他服务。
- 未连接真实 Casdoor、真实授权中心、真实 MySQL/Redis 或业务 VM。
- 未提交、未推送、未部署。

live gates（需在真实环境验证，当前均未执行）：

1. 真实 `sys_social` 中存在且仅存在一条目标负责人 OIDC 绑定，`tenant_id` 与会话租户一致。
2. 授权中心已配置 `docman:project:query` 的 SELF/owner scope，并识别 `ownerSubject.issuer/sub`。
3. HTTP provider + 服务凭据在 dev/release 可用，传输/协议/凭据故障映射为 503。
4. 端到端验证 401（无 canonical 会话）、403（负责人绑定缺失/显式 deny/scope mismatch）、503（授权中心故障）。
5. 拒绝路径确认无项目/文档业务写入与无 allow 请求。


## 9. 2026-10-29 窄修：OIDC 原始 issuer 与 canonical issuer key 分离

### 9.1 缺口

此前 `CasdoorOidcTokenVerifier` 直接把 ID Token 的原始 `iss` URL 作为 `CanonicalIdentity.issuer`
输出，登录编排又把它原样写入 `sys_social.auth_id`、Sa-Token 会话 extra 与 docman `ownerSubject`。
授权中心及已有消费者使用的 canonical issuer key 是独立字符串（非 URL），因此绑定与授权主体不一致。

### 9.2 实际修改

| 文件 | 改动 |
| --- | --- |
| `ruoyi-admin/.../identity/oidc/login/DocmanOidcLoginProperties.java` | 新增 `canonicalIssuer` 字段，绑定 `docman.oidc.canonical-issuer` / `DOCMAN_OIDC_CANONICAL_ISSUER`；无默认值、不从 issuer/用户名/请求推导；`isLoginConfigured()` 要求非空白 |
| `ruoyi-admin/src/main/resources/application.yml` | 增加 `canonical-issuer: ${DOCMAN_OIDC_CANONICAL_ISSUER:}` 占位，无字面值 |
| `ruoyi-admin/.../identity/oidc/VerifiedOidcToken.java` | 新增：验证结果只承载已校验的原始 `iss` URL、`sub`、`tenantId`，与 canonical 身份显式区分 |
| `ruoyi-admin/.../identity/oidc/CasdoorOidcTokenVerifier.java` | 返回类型改为 `VerifiedOidcToken`；仍只用原始 issuer URL 校验 JWKS、`iss`、`aud`/`azp`、时间与 nonce，canonical key 不参与 JWT 校验 |
| `ruoyi-admin/.../identity/oidc/login/OidcLoginService.java` | 验证成功后显式转换为 `CanonicalIdentity(canonicalIssuer, verifiedSub, tenantId)`；绑定、会话 canonical issuer/sub 均使用转换结果 |
| `ruoyi-admin/.../identity/IdentitySafetyGuard.java` | OIDC 启用时 canonical issuer 缺失即拒绝启动（含 local）；非 local 额外拒绝含 `mock/fixture/synthetic/dummy/fake/debug` 的 canonical issuer，以及直接复用原始 issuer URL 的配置 |
| `ruoyi-admin/.../identity/oidc/CasdoorOidcTokenVerifierTest.java` | 断言验证结果保留原始 URL；canonical key 作为 JWT `iss` 一律拒绝 |
| `ruoyi-admin/.../identity/oidc/OidcLoginServiceTest.java` | 断言绑定与会话使用 canonical key、且不等于原始 URL；新增 canonical issuer 缺失时 503 |
| `ruoyi-admin/.../identity/IdentitySafetyGuardTest.java` | 新增 local/dev/release 缺失 canonical issuer、非 local 合成值、非 local 复用原始 URL、local 显式配置放行等用例 |
| `ruoyi-admin/.../identity/CanonicalIdentityBindingServiceTest.java` | authId/binding 使用 canonical issuer key |
| `ruoyi-modules/ruoyi-docman/.../authz/DocProjectAuthorizationServiceTest.java` | `ownerSubject.issuer` 断言为 canonical issuer key |
| `ruoyi-modules/ruoyi-docman/.../authz/SocialOwnerSubjectResolverTest.java` | 反向解析使用 canonical issuer key |
| `ruoyi-admin/.../controller/OidcLoginControllerTest.java` | 补充响应不含 `id_token` / `canonical_issuer` 的断言 |

浏览器响应仍只返回本地会话 access token、client id 与 scope；不返回 ID Token、原始或 canonical issuer；
验证器与登录编排均不打印 token、JWT 或 client secret。

### 9.3 配置合同

- `DOCMAN_OIDC_ISSUER`：原始 issuer URL，仅用于 discovery/JWKS 与 ID Token `iss` 精确校验。
- `DOCMAN_OIDC_CANONICAL_ISSUER`：授权中心/已有消费者使用的 canonical issuer key，必须显式提供。
- `DOCMAN_OIDC_ENABLED=true` 时，任一环境缺少 canonical issuer 都会启动失败或登录 503；local 不做自动映射。
- 非 local 环境若 canonical issuer 含合成/调试含义或等于原始 issuer URL，启动失败。

### 9.4 验证

运行环境：Temurin JDK `17.0.17`（显式 `JAVA_HOME`）、Maven `3.9.11`、离线模式 `-o`。

```bash
mvn -o -pl ruoyi-admin -am -DskipTests test-compile
mvn -o -pl ruoyi-modules/ruoyi-docman -am -DskipTests test-compile

mvn -o -pl ruoyi-admin -am -DskipTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest='PkceTest,OidcLoginServiceTest,RestClientOidcTokenEndpointClientTest,OidcLoginControllerTest,IdentitySafetyGuardTest,CasdoorOidcTokenVerifierTest,CanonicalIdentityBindingServiceTest' \
  test

mvn -o -pl ruoyi-modules/ruoyi-docman -am -DskipTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest='DocProjectAuthorizationServiceTest,SocialOwnerSubjectResolverTest,DocProjectControllerTest,DocmanAuthorizationExceptionHandlerTest' \
  test
```

实际结果：

- 两次 `test-compile` 均 `BUILD SUCCESS`。
- OIDC 窄测：`Tests run: 80, Failures: 0, Errors: 0, Skipped: 0`。
  - `CasdoorOidcTokenVerifierTest` 13、`OidcLoginServiceTest` 20、`IdentitySafetyGuardTest` 19、
    `CanonicalIdentityBindingServiceTest` 10、`OidcLoginControllerTest` 7、`PkceTest` 4、
    `RestClientOidcTokenEndpointClientTest` 7。
- binding/ownerSubject 窄测：`Tests run: 42, Failures: 0, Errors: 0, Skipped: 0`。
  - `DocProjectAuthorizationServiceTest` 12、`SocialOwnerSubjectResolverTest` 19、
    `DocProjectControllerTest` 8、`DocmanAuthorizationExceptionHandlerTest` 3。

### 9.5 未做事项与 live gates

- 未连接真实 Casdoor、授权中心、MySQL/Redis 或业务 VM；未读取 live Secret。
- 未修改授权中心、SmartQuote、FinanceHub 或 VM；未新增数据库迁移。
- 既有 `sys_social` 绑定行必须由运维按 canonical issuer key 与已核验 sub 建立或迁移，本次未写入任何绑定。
- live 验证需确认：真实 ID Token `iss` 严格等于原始 issuer URL；绑定/会话/`ownerSubject` 均输出同一 canonical key；
  canonical issuer 缺失或配置为合成值时服务拒绝启动。
- 未提交、未推送、未部署。
