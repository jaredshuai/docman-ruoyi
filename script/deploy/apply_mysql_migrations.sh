#!/usr/bin/env bash
set -euo pipefail

MIGRATIONS_DIR="${MIGRATIONS_DIR:-script/sql/migrations/mysql}"
MYSQL_HOST="${MYSQL_HOST:?MYSQL_HOST is required}"
MYSQL_PORT="${MYSQL_PORT:?MYSQL_PORT is required}"
MYSQL_DB_NAME="${MYSQL_DB_NAME:?MYSQL_DB_NAME is required}"
MYSQL_USER="${MYSQL_USER:?MYSQL_USER is required}"
MYSQL_PASSWORD="${MYSQL_PASSWORD:?MYSQL_PASSWORD is required}"
MIGRATION_TABLE="${MIGRATION_TABLE:-docman_schema_migration}"

if [[ ! -d "${MIGRATIONS_DIR}" ]]; then
  echo "Migration directory not found: ${MIGRATIONS_DIR}" >&2
  exit 1
fi

MYSQL_ARGS=(
  --protocol=TCP
  --host="${MYSQL_HOST}"
  --port="${MYSQL_PORT}"
  --user="${MYSQL_USER}"
  --database="${MYSQL_DB_NAME}"
  --default-character-set=utf8mb4
)

mysql_exec() {
  MYSQL_PWD="${MYSQL_PASSWORD}" mysql "${MYSQL_ARGS[@]}" "$@"
}

escape_sql_literal() {
  printf "%s" "$1" | sed "s/'/''/g"
}

echo "Ensuring migration history table ${MIGRATION_TABLE} exists in ${MYSQL_DB_NAME}..."
mysql_exec <<SQL
CREATE TABLE IF NOT EXISTS \`${MIGRATION_TABLE}\` (
  \`version\` VARCHAR(128) NOT NULL COMMENT '迁移版本',
  \`checksum\` VARCHAR(64) NOT NULL COMMENT '文件校验和',
  \`applied_at\` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '执行时间',
  PRIMARY KEY (\`version\`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Docman schema migration history';
SQL

mapfile -t migration_files < <(find "${MIGRATIONS_DIR}" -maxdepth 1 -type f -name '*.sql' | sort)

if [[ ${#migration_files[@]} -eq 0 ]]; then
  echo "No migration files found under ${MIGRATIONS_DIR}. Nothing to do."
  exit 0
fi

for file in "${migration_files[@]}"; do
  version="$(basename "${file}")"
  checksum="$(sha256sum "${file}" | awk '{print $1}')"
  version_sql="$(escape_sql_literal "${version}")"
  checksum_sql="$(escape_sql_literal "${checksum}")"

  applied_checksum="$(mysql_exec -Nse "SELECT checksum FROM \`${MIGRATION_TABLE}\` WHERE version='${version_sql}' LIMIT 1")"
  if [[ -n "${applied_checksum}" ]]; then
    if [[ "${applied_checksum}" != "${checksum}" ]]; then
      echo "Migration checksum mismatch for ${version}. Expected ${applied_checksum}, current ${checksum}." >&2
      exit 1
    fi
    echo "Skipping already applied migration: ${version}"
    continue
  fi

  echo "Applying migration: ${version}"
  mysql_exec < "${file}"
  mysql_exec -e "INSERT INTO \`${MIGRATION_TABLE}\` (\`version\`, \`checksum\`) VALUES ('${version_sql}', '${checksum_sql}')"
  echo "Applied migration: ${version}"
done

echo "All MySQL migrations applied successfully."
