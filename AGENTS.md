# AGENTS.md

## Cursor Cloud specific instructions

### Overview

This is **RuoYi-Vue-Plus** v5.5.3, a multi-tenant enterprise admin system built with Spring Boot 3.5.9 + Java 17/21. It is a backend-only repository; the frontend ([plus-ui](https://gitee.com/JavaLionLi/plus-ui)) lives in a separate repo.

### Required Services

| Service | Default Config | Notes |
|---------|---------------|-------|
| **MySQL 8.0** | `localhost:3306`, user `root`, password `root`, database `ry-vue` | SQL init scripts in `script/sql/` |
| **Redis** | `localhost:6379`, password `ruoyi123` | Used by Redisson for caching, locks, sessions |
| **ruoyi-admin** | `localhost:8080` | Main Spring Boot app (dev profile) |

### Starting Services

```bash
# Start MySQL (if not already running)
sudo mysqld --user=mysql &
sudo chmod 755 /var/run/mysqld/

# Start Redis with configured password
sudo redis-server --daemonize yes --requirepass ruoyi123

# Build and run the application
cd /workspace
mvn clean install -DskipTests -P dev
java -jar ruoyi-admin/target/ruoyi-admin.jar --spring.profiles.active=dev &
```

### Key Gotchas

- **No Maven wrapper**: The project does not include `mvnw`. System Maven (`mvn`) must be installed (`sudo apt-get install -y maven`).
- **MySQL socket permissions**: After starting `mysqld`, you may need `sudo chmod 755 /var/run/mysqld/` for the client to connect.
- **Login API encryption**: The `/login` endpoint uses `@ApiEncrypt` (RSA+AES encryption). For API testing without the frontend, use unauthenticated endpoints like `/auth/code`, `/auth/tenant/list`, or `/v3/api-docs`.
- **Default active profile**: `dev` is the default Maven profile. The app config is in `ruoyi-admin/src/main/resources/application-dev.yml`.
- **Database init**: Four SQL scripts must be imported into `ry-vue` database: `ry_vue_5.X.sql`, `ry_job.sql`, `ry_workflow.sql`, `ry_docman.sql` (in `script/sql/`).

### Build & Test Commands

- **Build**: `mvn clean install -DskipTests -P dev`
- **Test**: `mvn test -P dev` (tests are tagged by profile; `skipTests=true` by default in pom.xml)
- **Run**: `java -jar ruoyi-admin/target/ruoyi-admin.jar --spring.profiles.active=dev`

### Verification Endpoints

- `GET /auth/code` — captcha generation (validates Redis connection)
- `GET /auth/tenant/list` — tenant list (validates MySQL connection)
- `GET /v3/api-docs` — OpenAPI 3.x spec
