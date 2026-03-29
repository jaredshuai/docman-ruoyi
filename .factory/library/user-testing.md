# User Testing

Testing surfaces, required tools, and concurrency guidance for this mission.

**What belongs here:** validation surfaces, setup expectations, runtime gotchas, concurrency guidance.

---

## Validation Surface

### Surface: Backend viewer API
- Primary tool: `curl`
- Target runtime: local source-run backend on `http://localhost:8080` with `local` profile for validation
- Key behaviors:
  - viewer ticket creation
  - viewer-url generation
  - content delivery
  - expiry and permission denial
  - config-driven enable/disable behavior
- Notes:
  - Initialize validation data with `.factory/init-viewer-validation.ps1`, which imports `script/sql/ry_vue_5.X.sql`, `script/sql/ry_docman.sql`, seeds one minimal docman project/document, and writes a sample local file under `.factory/runtime/docman-upload`
  - Run the backend from source with `local` profile, `server.port=8080`, `captcha.enable=false`, `snail-job.enabled=false`, `spring.boot.admin.client.enabled=false`, `docman.storage.localOnly=true`, and `docman.upload.localRoot=D:/codespace/docman-ruoyi/.factory/runtime/docman-upload`
  - Start-path success now requires liveness proof, not just a PID: inspect the matching `.factory/runtime/*.health.log` file and ensure it records both the first healthy response and the settle-window confirmation before using the service
  - Use targeted test fixtures and controlled TTL values for expiry checks
  - For `VAL-VIEWER-ACCESS-002`, stop only the local validation backend and relaunch it with the same startup command plus `-Ddocman.viewer.enabled=false`; keep the viewer and plus-ui runtimes untouched so the disabled-mode check is isolated to the backend process

### Surface: Frontend docman document list
- Primary tool: `agent-browser`
- Target repo: `D:\codespace\docman-plus-ui`
- Entry point under test: `src/views/docman/document/index.vue`
- Key behavior:
  - “在线预览” action calls backend viewer-url and navigates to the returned external viewer address
- Notes:
  - This surface is intentionally minimal
  - Browser validation is secondary to backend API validation for this mission
  - The sibling frontend repo currently has unrelated global `vue-tsc` failures; for this mission, rely on touched-file lint, build, and browser evidence rather than repo-wide type-cleanup
  - Start the local Vite server on `http://localhost:3101`; its dev proxy already targets host backend `http://localhost:8080`
  - A temporary placeholder viewer on `http://localhost:8012` is allowed for this milestone only to prove redirect/navigation and query-string composition (`src`, `mode`); it is not evidence that a real external viewer can render content
  - Final delivery must clearly distinguish verified redirect/navigation from unverified real-viewer rendering

## Validation Concurrency

### curl / API validation
- Max concurrent validators: **5**
- Rationale:
  - API checks are lightweight
  - Machine capacity observed during planning: 24 CPU, ~29.6 GiB RAM total, ~10.5 GiB free
  - Even under the 70% headroom rule, API validation stays well within budget

### agent-browser / frontend validation
- Max concurrent validators: **3**
- Rationale:
  - Browser validation multiplies browser memory, frontend dev-server overhead, and backend activity
  - Existing local Java containers are already running, so stay conservative
  - Three concurrent browser validators fit comfortably within the observed free-memory envelope

## Mission-Specific Gotchas

- Existing historical unit tests around `RedisUtils` static initialization are not part of this mission's validation target unless directly impacted.
- Existing global `vue-tsc` failures in `D:\codespace\docman-plus-ui` are treated as pre-existing unless the preview-entry change adds a new local regression.
- On Windows, launch the frontend through the `docman-plus-ui` service in `.factory/services.yaml` for browser validation instead of ad hoc foreground Exec runs.
- Use the `docman-viewer-placeholder` service in `.factory/services.yaml` only for the browser redirect assertion when the user has not provided a real external viewer.
- Service stop commands in `.factory/services.yaml` use PowerShell `Get-NetTCPConnection` + `Stop-Process` for the declared ports `8080`, `3101`, and `8012`; prefer those manifest commands over ad hoc `netstat` parsing.
- External `document` viewer retries the same `src`; validators must treat repeated content fetches within TTL as expected behavior.
- Preview must never reveal storage internals, even in error payloads or headers.
