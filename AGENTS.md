# Agent Notes

## Local Infra Reference

Sensitive infra details for this repo should live in `AGENTS.local.md`, not here.

Rules:

- Read `AGENTS.local.md` first when the task involves deploy, release, SSH, MySQL, Redis, SnailJob, or other environment-specific coordination.
- Do not copy secrets from `AGENTS.local.md` into tracked files.
- Do not commit `AGENTS.local.md`.

If `AGENTS.local.md` is missing or stale, re-check these sources:

- `.github/workflows/deploy-backend.yml`
- `/srv/docman/dev/backend/docker-compose.yml` on `10.34.200.102`
- `/srv/docman/release/backend/docker-compose.yml` on `10.34.200.102`
- `ruoyi-admin/src/main/resources/application-prod.yml`

## Deploy Branches

- Backend deploy workflow listens to `dev` and `release`
- `main` does not auto-deploy by itself
