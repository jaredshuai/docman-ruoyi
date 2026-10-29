# Canonical Identity Binding Service（只读接缝）交付说明

- 隔离 worktree：`C:\Users\jared\AppData\Local\Temp\docman-java-consumer-0.1.1-20261027`
- 范围：仅在 `ruoyi-admin` 的 web service 边界内新增一个只读 service 与两个 record，复用现有 `sys_social` 绑定表与 `ISysSocialService`。
- 明确未做：**没有新增表/字段，没有 OIDC/JWKS/Casdoor 调用，没有连接任何 live 环境，没有自动 insert/update/delete 绑定，没有修改 docman controller/service，没有接入项目 PEP，没有构造 Principal，没有读取请求 Header/Cookie。**

## 1. 改动文件

| 文件 | 类型 | 说明 |
| --- | --- | --- |
| `ruoyi-admin/src/main/java/org/dromara/web/service/identity/CanonicalIdentity.java` | 新增 | 规范化身份 record（issuer、subject、tenantId），空白输入返回 `Optional.empty()`；生成稳定且带命名空间的 `authId`。 |
| `ruoyi-admin/src/main/java/org/dromara/web/service/identity/CanonicalIdentityBinding.java` | 新增 | 最小本地用户标识 record（userId、tenantId）。 |
| `ruoyi-admin/src/main/java/org/dromara/web/service/identity/CanonicalIdentityBindingService.java` | 新增 | `@Service` 只读查询服务，注入 `ISysSocialService` 与 `SysUserMapper`。 |
| `ruoyi-admin/src/test/java/org/dromara/web/service/identity/CanonicalIdentityBindingServiceTest.java` | 新增 | 纯单元测试，Mockito mock 依赖，不启动 Spring/DB/Redis。 |
| `identity-binding-delivery.md` | 新增 | 本交付文档。 |

未触碰：`ruoyi-modules/ruoyi-docman/**`、`LoginUser.java`、`LoginHelper.java`、`ruoyi-modules/ruoyi-docman/pom.xml`（保留既有未提交改动与 `jxccs-authz-client:0.1.1` 依赖）。

## 2. 查找接缝

实现入口：

```java
Optional<CanonicalIdentityBinding> resolve(String issuer, String subject, String tenantId)
```

- 输入任一为 `null`、空串或纯空白：立即返回 `Optional.empty()`，不触达任何 service/mapper。
- `authId` 组合（`CanonicalIdentity.authId()`）：

  ```text
  oidc:iss=<base64url(issuer)>:sub=<base64url(subject)>
  ```

  - 固定命名空间 `oidc:iss=`，与 JustAuth 现有 `source + uuid` 组合区分。
  - issuer 与 subject 必须同时参与；相同 subject 不同 issuer 会得到不同 `authId`。
  - 各段做 Base64 URL 无填充编码，避免分隔符注入/歧义。
  - `tenantId` 不进入 `authId`，在候选过滤阶段校验。
  - 不使用 `username`、`userId`、`openId` 代替 subject。

- 查询：`ISysSocialService.selectByAuthId(authId)`（现有绑定表查询，无新增 SQL）。

## 3. Fail-closed 规则

以下情况全部返回 `Optional.empty()`：

1. issuer / subject / tenantId 任一为空白。
2. 绑定查询返回 `null` 或空列表（未知绑定）。
3. 按 `tenantId` 过滤后候选数为 0（租户不匹配）。
4. 按 `tenantId` 过滤后候选数大于 1（同租户多绑定冲突）。
5. 绑定记录 `userId` 为空。
6. `SysUserMapper.selectVoById(userId)` 查不到用户。
7. 用户状态为 `SystemConstants.DISABLE`（停用）。
8. 用户租户与请求租户不一致。

命中时只返回 `CanonicalIdentityBinding(userId, tenantId)`，不返回完整用户对象，不写入任何状态。

## 4. 验证

运行环境：

- JDK：Temurin `17.0.17`（显式 `JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-17.0.17.10-hotspot`）
- Maven：`3.9.11`，离线 `-o`

### 4.1 定向编译

```bash
mvn -o -pl ruoyi-admin -am -DskipTests compile
```

结果：`BUILD SUCCESS`（`-q` 静默执行，无错误）。

### 4.2 定向单元测试

```bash
mvn -o -pl ruoyi-admin -am -DskipTests=false \
  -Dtest=CanonicalIdentityBindingServiceTest \
  -Dsurefire.failIfNoSpecifiedTests=false test
```

结果：`Tests run: 10, Failures: 0, Errors: 0, Skipped: 0`，`BUILD SUCCESS`。

覆盖用例：

| 用例 | 断言 |
| --- | --- |
| 空输入 | null/空串/纯空白分别返回 empty，且无任何 mock 交互 |
| issuer+sub 命名空间 | authId 带 `oidc:iss=`/`:sub=` 前缀，issuer 或 subject 变化即变化，tenant 不影响 authId |
| 绑定命中 | 返回对应 userId/tenantId，且只调用一次 `selectByAuthId` 与一次用户查询，无写操作 |
| tenant 不匹配 | 返回 empty，不查询用户 |
| 停用用户 | 返回 empty |
| 同租户多绑定 | 返回 empty，不查询用户 |
| 跨租户多绑定且仅一个匹配 | 命中唯一匹配项 |
| 未知绑定 | 返回 empty，不查询用户 |
| 用户不存在 | 返回 empty |
| 用户租户不一致 | 返回 empty |

测试类仅使用 JUnit 5 + Mockito 扩展，未启动 Spring 上下文、数据库或 Redis。

## 5. 未做事项与边界

- 未新增或修改数据库 schema、迁移、表结构。
- 未实现绑定创建/更新/删除、自动绑定、冲突修复、账号合并。
- 未实现 OIDC 登录、JWKS 校验、token 解析、Casdoor/授权中心调用。
- 未从请求 Header、Cookie、Principal 或 Sa-Token 会话读取身份。
- 未修改 `SocialAuthStrategy`、`SysLoginService`、`AuthController` 或任何 docman controller/service。
- 未将本服务注册为 PEP 或登录策略；它只是未来可信 OIDC/Authz 登录策略可复用的 `(issuer, sub) -> local userId` 只读接缝。
- 未提交、未推送、未部署、未连接 live。

## 6. 脱敏声明

本交付未记录任何密码、token、JWT secret、client secret、kubeconfig 或带凭据 URL；未连接 Casdoor/k3s/业务 VM/真实授权中心。
