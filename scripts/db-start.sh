#!/bin/sh
set -eu
. "$(dirname "$0")/env.sh"
umask 077
ERP_DB_BIN=${ERP_DB_BIN:-/opt/homebrew/opt/mariadb@11.4/bin}
ERP_LOCAL="$ERP_ROOT/.local"
mkdir -p "$ERP_LOCAL"
ERP_SOCKET="$ERP_LOCAL/mariadb.sock"
if [ ! -d "$ERP_LOCAL/data/mysql" ]; then
  "$ERP_DB_BIN/mariadb-install-db" --no-defaults --datadir="$ERP_LOCAL/data" --auth-root-authentication-method=socket --skip-test-db > "$ERP_LOCAL/install.log" 2>&1
fi
if ! "$ERP_DB_BIN/mariadb-admin" --no-defaults --socket="$ERP_SOCKET" ping >/dev/null 2>&1; then
  nohup "$ERP_DB_BIN/mariadbd" --no-defaults --datadir="$ERP_LOCAL/data" --socket="$ERP_SOCKET" --pid-file="$ERP_LOCAL/mariadb.pid" --port=3307 --bind-address=127.0.0.1 --innodb-buffer-pool-size=128M --max-connections=15 --performance-schema=OFF --log-error="$ERP_LOCAL/mariadb.log" > "$ERP_LOCAL/server.out" 2>&1 &
  ERP_ATTEMPT=0
  until "$ERP_DB_BIN/mariadb-admin" --no-defaults --socket="$ERP_SOCKET" ping >/dev/null 2>&1; do
    ERP_ATTEMPT=$((ERP_ATTEMPT + 1))
    if [ "$ERP_ATTEMPT" -ge 30 ]; then cat "$ERP_LOCAL/mariadb.log"; exit 1; fi
    sleep 1
  done
fi
if [ ! -f "$ERP_LOCAL/db.env" ]; then
  ERP_APP_PASSWORD=$(openssl rand -hex 24)
  ERP_TEST_PASSWORD=$(openssl rand -hex 24)
  "$ERP_DB_BIN/mariadb" --no-defaults --socket="$ERP_SOCKET" <<SQL
CREATE DATABASE IF NOT EXISTS publishing_erp CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS publishing_erp_test CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS 'erp_app'@'127.0.0.1' IDENTIFIED BY '$ERP_APP_PASSWORD';
CREATE USER IF NOT EXISTS 'erp_test'@'127.0.0.1' IDENTIFIED BY '$ERP_TEST_PASSWORD';
GRANT ALL PRIVILEGES ON publishing_erp.* TO 'erp_app'@'127.0.0.1';
GRANT ALL PRIVILEGES ON publishing_erp_test.* TO 'erp_test'@'127.0.0.1';
SQL
  cat > "$ERP_LOCAL/db.env" <<ENV
DB_URL=\${DB_URL:-jdbc:mariadb://127.0.0.1:3307/publishing_erp}
DB_USERNAME=\${DB_USERNAME:-erp_app}
DB_PASSWORD=\${DB_PASSWORD:-$ERP_APP_PASSWORD}
TEST_DB_URL=\${TEST_DB_URL:-jdbc:mariadb://127.0.0.1:3307/publishing_erp_test}
TEST_DB_USERNAME=\${TEST_DB_USERNAME:-erp_test}
TEST_DB_PASSWORD=\${TEST_DB_PASSWORD:-$ERP_TEST_PASSWORD}
ENV
fi
printf '%s\n' 'MariaDB ready: 127.0.0.1:3307 (project-local data)'
