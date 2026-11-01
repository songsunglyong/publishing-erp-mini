#!/bin/sh
# Source only from project scripts.
ERP_ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
cd "$ERP_ROOT"
if [ -z "${JAVA_HOME:-}" ]; then
  # 프로젝트 전용 보안 패치 JDK를 우선한다. 명시한 JAVA_HOME은 그대로 존중한다.
  ERP_PROJECT_JDK="$ERP_ROOT/.local/jdks/temurin-21.0.13+9/Contents/Home"
  if [ -x "$ERP_PROJECT_JDK/bin/java" ]; then
    JAVA_HOME="$ERP_PROJECT_JDK"
  elif [ -d /opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home ]; then
    JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home
  fi
fi
if [ -n "${JAVA_HOME:-}" ]; then
  export JAVA_HOME
  PATH="$JAVA_HOME/bin:$PATH"
  export PATH
fi
if [ -f .local/db.env ]; then
  set -a
  . .local/db.env
  set +a
fi

if [ -f .local/auth.env ]; then
  set -a
  . .local/auth.env
  set +a
fi
