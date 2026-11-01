#!/bin/sh
# 비밀 환경변수는 출력하지 않고 프로젝트가 선택한 Java만 확인한다.
set -eu
. "$(dirname "$0")/env.sh"
exec java -version
