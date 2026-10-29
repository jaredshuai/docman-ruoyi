# Canonical Identity Carrier — 交付说明(脱敏)

日期: 2026-10-27
范围: 隔离 worktree `docman-java-consumer-0.1.1-20261027`,仅本地代码与编译验证
状态: 未提交、未推送、未部署、未连接任何 live 环境

## 1. 本次交付是什么

**Session carrier preparation(会话载体准备)**,且仅是这一步:

- 在 `LoginUser` 上为 canonical principal 的 `issuer` / `sub` 增加两个**可选运行时字段**,使其可以随会话对象流经既有登录管道。
- 在 `LoginHelper` 上增加配套的 extra key 常量、`login()` 内的**条件性** extra 写入、以及两个**只读** getter。

## 2. 本次交付明确不是什么

- **不是 OIDC 接入**:没有实现授权码/ID Token 校验、没有接触任何 issuer 发现或 JWKS。
- **不是授权中心完成**:未实现 binding 表、未接入 Casdoor、未新增任何业务权限、未实现项目详情 PEP。
- **不是 live 变更**:未连接 Redis/MySQL/生产或 dev 环境,未读取或输出任何凭据。

当前代码库中**不存在可信 OIDC 来源**,因此密码/短信/social 等全部现有登录路径都不会为这两个字段赋值,字段在所有现存登录行为中保持 `null`。

## 3. 变更清单

### 3.1 `ruoyi-common/ruoyi-common-core` — `LoginUser.java`(+12 行)

- 新增字段 `canonicalIssuer`、`canonicalSubject`(`String`,Lombok `@Data` 生成访问器)。
- 字段为纯载体:不参与 `getLoginId()`(仍为 `userType + ":" + userId`),不替代、不遮蔽 `userId`/`username`,无默认值,无任何从 username/userId 的推导逻辑。

### 3.2 `ruoyi-common/ruoyi-common-satoken` — `LoginHelper.java`(+24 行)

- 新增常量 `CANONICAL_ISSUER_KEY = "canonicalIssuer"`、`CANONICAL_SUBJECT_KEY = "canonicalSubject"`(与既有 `userId`/`userName` 等 key 互不复用)。
- `login()` 中仅当调用方在 `LoginUser` 上**显式设置了非空白值**时才 `setExtra` 进入 token/session extra;缺失或空白值保持缺失,不写入 null 占位。
- 新增只读 getter `getCanonicalIssuer()` / `getCanonicalSubject()`,沿用既有 `getExtra` 读取模式(无上下文/无该 key 时返回 `null`)。
- **没有**任何从请求参数、Cookie、普通 Header 或本地用户名推导这些值的代码。

### 3.3 测试文件状态

本轮没有留下新的测试源文件。编码 AI 的早期交付文本曾声称存在 `CanonicalIdentityCarrierTest`，但独立检查确认该文件不在 worktree，因此不采信那组 5 个测试数字，也不把它们写成已通过证据。

## 4. 验证记录(JDK 17,Temurin 17.0.17)

- 模块级编译:`ruoyi-common-core` + `ruoyi-common-satoken` — BUILD SUCCESS。
- 独立复核：`javap` 确认 LoginUser 保留 `getLoginId()/getUserId()/getUsername()`，并新增 canonical getter；LoginHelper 保留 `login()`，并新增 canonical getter/key。
- Java17 模块编译：`ruoyi-common-core`、`ruoyi-common-redis`、`ruoyi-common-satoken` 5 模块 `BUILD SUCCESS`。
- 没有重新运行 docman 全量测试；早期交付文本中的 613/612 数字未被采信。

### 既有失败(与本次改动无关,改动前即存在)

`ruoyi-docman` 全量套件中 4 failures + 1 error,均与身份载体无关:

- `DocMapperXmlContractTest`(3 项):类路径中存在陈旧构建产物残留的 `DocProjectAddRecordDetailMapper_1775182799865.xml` 等 mapper XML(源码目录中并无该文件),及 `${ew.customSqlSegment}` 占位符契约告警 — 属打包 worktree 的 target/ 残留与环境问题。
- `DocProjectServiceImplTest`(1 error):mock 的 `selectPage` 返回 null 导致 `TableDataInfo.build(page)` NPE — 属该测试自身桩数据问题,与 LoginUser/LoginHelper 无交集。

判定依据:本轮独立证据仅覆盖源码 diff、Java17 核心模块编译和公开方法检查；没有把未实际存在的测试文件或未重新执行的全量测试作为通过依据。本次修改的两个主代码文件仅新增可选字段与条件写入,不改变既有 `getLoginId()` 行为。

## 5. 边界与后续(不属于本票)

真实 OIDC/授权中心接入至少还需要(本票一律未做):

- 可信 issuer 的发现与 ID Token 验签(JWKS)——需授权中心方提供权威 issuer 配置,禁止猜测。
- canonical principal(issuer+sub)与本地 `sys_user` 的 binding 表及建绑/冲突策略。
- 登录策略(如 social/OIDC strategy)中**由可信验签结果**显式调用 `loginUser.setCanonicalIssuer/Sub(...)`,这是本载体唯一的合法写入方。
- 项目详情等场景的 PEP/权限决策逻辑。

## 6. Git 状态

- 所有变更保留在本隔离 worktree 工作区,未 `git add`、未 commit、未 push。
- 本 worktree 中另有非本票产生的既有未提交痕迹(`ruoyi-docman/pom.xml` 的 authz-client 0.1.1 依赖、`delivery.md`、`identity-wiring-audit.md`、`target/` 残留),本次未触碰、未纳入任何交付主张。
