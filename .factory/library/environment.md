# Environment

Environment variables, external dependencies, and setup notes.

**What belongs here:** required config keys, external services, sibling repo paths, dependency quirks.
**What does NOT belong here:** service commands or ports matrix beyond factual references.

---

## Repositories

- Backend repo: `D:\codespace\docman-ruoyi`
- Frontend repo: `D:\codespace\docman-plus-ui`

## Existing local runtime

- Existing docker `docman-ruoyi-admin` is exposed on `http://localhost:18081`, but current user-testing should use a parallel local source-run backend on `http://localhost:8080`
- `docman-mysql` is already exposed on `localhost:3307`
- `docman-redis` is already exposed on `localhost:6380`
- `docman-snailjob` is already exposed on `localhost:18800`
- Temporary placeholder viewer for redirect validation may run on `http://localhost:8012`

Do not repurpose ports `7201`, `5432`, or `6379`.

## Viewer configuration to add/use

- `docman.viewer.enabled`
- `docman.viewer.base-url`
- `docman.viewer.ticket-ttl-seconds`

Recommended defaults for local validation:

- `docman.viewer.enabled=true`
- `docman.viewer.ticket-ttl-seconds=300`
- `docman.viewer.base-url=http://localhost:8012` when using the temporary placeholder viewer for redirect testing

## Redis usage

- Reuse the existing Redis deployment
- Ticket key prefix: `docman:viewer:ticket:`
- Ticket reuse within TTL is intentional; do not implement one-time-consume semantics

## Frontend local note

`docman-plus-ui` already has `node_modules` installed and uses Vite. The document list page exists under `src/views/docman/document/index.vue`.

For local browser validation, the current Vite dev proxy targets host backend `http://localhost:8080`.
Keep the validation runtime aligned with that host backend rather than the stale docker mapping on `18081`.
