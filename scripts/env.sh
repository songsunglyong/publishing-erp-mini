#!/bin/sh
# Source only from project scripts.
ERP_ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
cd "$ERP_ROOT"
if [ -z "${JAVA_HOME:-}" ] && [ -d /opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home ]; then
  JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home
  export JAVA_HOME
fi
if [ -f .local/db.env ]; then
  set -a
  . .local/db.env
  set +a
fi
