# Direct Casdoor OIDC ID Token 验证器 — 交付说明(脱敏)

日期: 2026-10-28
范围: 隔离 worktree `C:\Users\jared\AppData\Local\Temp\docman-java-consumer-0.1.1-20261027`,仅本地代码与编译/测试验证
状态: 未提交、未推送、未部署、未连接 Casdoor 或任何 live 环境,未读取或输出任何敏感配置值

## 1. 本次交付是什么

第一窄票:**标准 ID Token 验证器与显式配置合同**。

- 新增显式 OIDC 配置模型 `DocmanOidcProperties`,无任何默认值。
- 新增 `CasdoorOidcTokenVerifier`,使用 Spring Security JOSE 的 `NimbusJwtDecoder` + 标准 `JwtValidators` 完成签名/JWKS、issuer、时间与 audience(必要时 azp)校验,输出可信的 `CanonicalIdentity`。
- 新增完全离线的纯单元测试,使用内存内 RSA 密钥对签名,不访问网络。

## 2. 本次交付明确不是什么

- **不是授权码流程**:未实现 login controller、authorization code exchange、redirect。
- **不是 Casdoor 客户端**:未实现 Casdoor client、secret 管理、token endpoint 调用。
- **不是绑定**:不调用 `CanonicalIdentityBindingService`,不做自动绑定或用户解析。
- **不是 PEP/权限**:未实现项目详情等场景的权限决策。
- **不是 live 变更**:未连接 Casdoor/JWKS/Redis/MySQL/生产或 dev,未读取或输出凭据。

## 3. 变更清单(全部为本票新增文件)

### 3.1 `ruoyi-admin/src/main/java/org/dromara/web/service/identity/oidc/DocmanOidcProperties.java`

- record 配置合同,字段:`enabled`、`issuer`、`jwksUri`、`audience`、`tenantId`。
- **不提供任何默认值**,不包含 client secret 字段。
- `isConfigured()` 仅在 `enabled == true` 且 `issuer/jwksUri/audience/tenantId` 均非空白时返回 true;否则一律视为未配置。
- `clientId()` 返回 `audience`,明确二者是同一 Casdoor client_id,仅作命名便利。

### 3.2 `ruoyi-admin/src/main/java/org/dromara/web/service/identity/oidc/AudienceValidator.java`

- 实现标准 `OAuth2TokenValidator<Jwt>`。
- `aud` 必须包含期望 client_id;`aud` 含多个值时要求 `azp` 显式等于期望 client_id;单个 audience 但 `azp` 存在且不匹配时同样拒绝。

### 3.3 `ruoyi-admin/src/main/java/org/dromara/web/service/identity/oidc/CasdoorOidcTokenVerifier.java`

- 生产构造按 `jwksUri` 构建 `NimbusJwtDecoder.withJwkSetUri(...)`;**未完整配置时不构建解码器**。
- 统一装配校验器:`JwtValidators.createDefaultWithIssuer(issuer)`(issuer + 时间)+ `AudienceValidator`。
- `verify(String idToken)`:
  - 配置缺失/未启用、解码器为 null、输入为 null/空白 → 立即 `Optional.empty()`。
  - 通过 `JwtDecoder.decode` 完成签名与 JWKS 校验(库实现,无手写 base64/签名验证)。
  - 解码成功后再以原始 `iss` claim 与配置 issuer **精确比较**,不一致即拒绝。
  - 仅用标准 `sub` claim 构造 `CanonicalIdentity.of(issuer, sub, tenantId)`;`sub` 缺失/空白时 empty。
  - 捕获 `JwtException`/`IllegalArgumentException`,任何异常 fail closed。
- 不生成 `Principal`/`Authentication`,不把 username/userId/employeeId 当作 subject,不调用绑定服务。
- 另提供包级构造器用于注入内存 `NimbusJwtDecoder`,便于离线测试;校验器仍由本类统一装配。

### 3.4 `ruoyi-admin/src/test/java/org/dromara/web/service/identity/oidc/CasdoorOidcTokenVerifierTest.java`

纯单元测试,共 12 个用例,全部离线:

1. 空白 token 拒绝
2. 配置缺失(properties 为 null、enabled=false、issuer/jwks/audience/tenant 空白)即使 token 有效也拒绝
3. 有效 token 返回 exact issuer/sub/tenant
4. 错误 issuer 拒绝
5. 错误 audience 拒绝
6. 多 audience 时 azp 缺失/错误拒绝、正确 azp 通过
7. 单 audience 但 azp 不匹配拒绝
8. 过期 token 拒绝
9. 非法 token(`not-a-jwt`、`a.b.c`、`alg=none` 形态)拒绝
10. 由外部私钥签名的 token 拒绝(签名校验)
11. 缺少 `sub` 但携带 username/userId/employeeId 时拒绝
12. `sub` 与 username/employeeId 同时存在时,subject 取自 `sub`

测试固件全部在内存生成:JDK `KeyPairGenerator`(RSA 2048)+ Nimbus `RSASSASigner` 签名,`NimbusJwtDecoder.withPublicKey(...)` 验签;无任何网络访问。

## 4. 依赖

- 本票在 `ruoyi-admin/pom.xml` 中接入 `spring-security-oauth2-jose` 依赖；未引入 security filter chain，未加入 client secret 或 live endpoint。
- 该依赖由 Spring Boot 3.5.9 管理为 Spring Security 6.5.7,其传递依赖 `com.nimbusds:nimbus-jose-jwt:9.37.4` 在首次构建时从公共 Maven 镜像下载(公共构建产物,非 Casdoor/live)。

## 5. 验证记录(JDK 17)

环境:`JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-17.0.17.10-hotspot`,Maven 3.9.11。

命令(仅 ruoyi-admin 模块):

```
mvn -pl ruoyi-admin test -DskipTests=false -Dtest=CasdoorOidcTokenVerifierTest -DfailIfNoTests=false
```

结果:

```
[INFO] Running org.dromara.web.service.identity.oidc.CasdoorOidcTokenVerifierTest
[INFO] Tests run: 12, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

说明:

- 父 POM 默认 `<skipTests>true</skipTests>`,因此显式传 `-DskipTests=false`;首次不带该参数时出现 `Tests are skipped`,未计入通过证据。
- 编译阶段显示 `Compiling 24 source files with javac [debug target 17]`,确认 Java 17 目标。
- 本次未运行全量测试;没有把未执行的全量套件作为通过依据。

## 6. 边界与后续(不属于本票)

- 授权码 callback、login controller、Casdoor client、redirect、secret 管理。
- 可信验签结果写入 `LoginUser.setCanonicalIssuer/Sub` 的登录策略接线。
- canonical identity 与本地 `sys_user` 的绑定/冲突策略及项目 PEP。
- 生产环境 `DocmanOidcProperties` 的配置绑定与 Bean 装配(本票刻意未注册为 Spring Bean,避免自动激活或隐式网络访问)。

## 7. Git 状态

- 所有新增文件保留在隔离 worktree 工作区,未 `git add`、未 commit、未 push。
- 未触碰 worktree 中既有的其他未提交改动。
