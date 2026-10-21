#!/bin/sh
# Local-only credentials; never print the generated password to logs or commit it.
set -eu
ERP_AUTH_ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
cd "$ERP_AUTH_ROOT"
umask 077
mkdir -p .local
if [ -e .local/auth.env ]; then
  printf '%s\n' '.local/auth.env already exists; unchanged.'
  exit 0
fi
ERP_INITIAL_SECRET=$(openssl rand -hex 24)
cat > .local/auth.env <<ENV
ERP_BOOTSTRAP_ENABLED=true
ERP_BOOTSTRAP_USERNAME=admin
ERP_BOOTSTRAP_PASSWORD=$ERP_INITIAL_SECRET
ENV
chmod 600 .local/auth.env
printf '%s\n' 'Created .local/auth.env (mode 600). Open locally to read credentials; do not share or commit.'
