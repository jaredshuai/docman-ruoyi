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

For the disabled-mode validation surface, keep the shared `local` profile enabled and launch the backend with an isolated JVM override instead:

- `-Ddocman.viewer.enabled=false`

This override is meant only for the validation harness so `VAL-VIEWER-ACCESS-002` can be exercised without permanently editing `application-local.yml`.

## Redis usage

- Reuse the existing Redis deployment
- Validation seeding normalizes the local `docman-redis` instance to require password `ruoyi123`; shared healthchecks and local validation clients must authenticate with that password after running `.factory/init-viewer-validation.ps1`
- Ticket key prefix: `docman:viewer:ticket:`
- Ticket reuse within TTL is intentional; do not implement one-time-consume semantics

## Frontend local note

`docman-plus-ui` already has `node_modules` installed and uses Vite. The document list page exists under `src/views/docman/document/index.vue`.

For local browser validation, the current Vite dev proxy targets host backend `http://localhost:8080`.
Keep the validation runtime aligned with that host backend rather than the stale docker mapping on `18081`.

## Windows PowerShell runtime rule

The validation scripts run under `powershell.exe` 5.1. Native commands such as `docker`, `mvn`, `npm`, and `py` do **not** throw into `catch` just because they exit non-zero. Runtime scripts must branch on `$LASTEXITCODE` / `$?` for native process failures, and startup scripts should record explicit health/settle evidence in `.factory/runtime/*.health.log` before returning a PID.
