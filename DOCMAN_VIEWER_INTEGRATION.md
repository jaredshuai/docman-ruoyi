# Docman Viewer 集成说明

## 已实现

- 后端已提供 `viewer-ticket`、`viewer-url`、`viewer/content/{ticket}` 预览链路。
- `docman.viewer.enabled`、`docman.viewer.base-url`、`docman.viewer.ticket-ttl-seconds` 可控制本地与部署环境行为。
- `viewer-url` 返回外部文档 viewer 地址，前端只消费该结果，不拼接 OSS、本地路径或 NAS 路径。
- `viewer-url.data.src` 是**绝对后端地址**，格式类似 `https://backend.example.com:18081/docman/document/viewer/content/{ticket}`；外部 viewer 必须直接请求该地址，不能假设 `src` 是相对路径，也不能依赖 viewer 与后端同源。
- Redis 票据键设计为 `docman:viewer:ticket:{token}`，默认 TTL 为 `300` 秒，TTL 内允许重复读取，便于外部 viewer 重试。

## 仅预留

- `mode=edit` 目前未启用。
- `saveUrl`、`saveToken` 当前仅作为保留字段返回 `null`，本次未实现保存回调、回写鉴权、冲突处理。
- 本次任务仅完成在线预览，不宣称完整在线编辑能力。

## 后端配置

### `application-local.yml` 示例

```yml
docman:
  viewer:
    enabled: true
    ticket-ttl-seconds: 300
    base-url: http://localhost:8012
```

- `enabled=false` 时，`viewer-ticket` 与 `viewer-url` 会拒绝请求。
- `base-url` 必须指向独立部署的外部文档 viewer 服务。
- `base-url` 只决定外部 viewer 的入口地址；实际文档内容仍从 backend 的 `viewer/content/{ticket}` 绝对地址读取。
- `ticket-ttl-seconds` 建议保持短期有效；外部 viewer 若会重试，TTL 内复用同一 `src` 是受支持的。

## 本地联调

### `docker-compose.local.yml`

- 后端本地容器编排已复用：
  - MySQL: `3307`
  - Redis: `6380`
  - Admin: `18081`
- 外部文档 viewer 不在当前 compose 中内置，需要单独部署，并把 `docman.viewer.base-url` 指向它。

### 本地 profile

- 可使用 `local` profile 加载 `ruoyi-admin/src/main/resources/application-local.yml`。
- 该示例已将 MySQL/Redis 指向本地 compose 端口，并保留 viewer 三个关键配置项。

## 前端联动

- sibling 前端仓库 `D:/codespace/docman-plus-ui` 已按最小范围接入现有文档列表页。
- 前端入口调用 `GET /docman/document/{id}/viewer-url`，再跳转到返回的外部 viewer 地址。
- 前端不应自行拼接存储地址，也不应依赖 OSS 原始 URL。
- 当前预览链路面向 standalone viewer 部署：viewer 在独立域名/端口运行时，只要能访问 backend 返回的绝对 `src` 即可，不要求通过同源代理转发文档流。

## 外部 viewer 部署预期

- 外部 viewer 负责渲染文档内容。
- backend 负责鉴权、签发票据、返回受保护的 `src`。
- viewer 访问的内容地址应来自 `viewer-url` 中编码后的 `src`，而不是直接访问存储层。
- 若 backend 依赖浏览器登录 Cookie/Sa-Token 会话鉴权，外部 viewer 请求 `src` 时也必须带上该登录态；部署时通常需要保证 viewer 页面与 backend 属于可共享 Cookie 的站点布局，并允许带凭证的跨站请求。
- 若运行环境不能安全共享登录态，本次任务未提供额外 token 透传机制，应在后续编辑/回写方案中统一设计 viewer 专用鉴权。

## 预览限制

- 仅支持 `preview` 模式。
- 票据过期后需要重新请求 `viewer-url` 或 `viewer-ticket`。
- 当前已实现的是“外部 viewer + backend 绝对 src”预览模式，不表示支持同源静态代理、OSS 直链或免鉴权公开访问。
- 未实现编辑回写、保存回调、编辑锁或版本冲突处理。

## 下一步扩展建议

- 在后续迭代中，为 `mode=edit` 增加 `saveUrl` 与 `saveToken` 的真实回调协议。
- 保存回调建议继续复用项目权限模型，并对回写内容做版本校验与审计记录。
