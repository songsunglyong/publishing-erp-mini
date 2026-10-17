#!/bin/sh
set -eu
. "$(dirname "$0")/env.sh"
ERP_DB_BIN=${ERP_DB_BIN:-/opt/homebrew/opt/mariadb@11.4/bin}
"$ERP_DB_BIN/mariadb-admin" --no-defaults --socket="$ERP_ROOT/.local/mariadb.sock" shutdown
