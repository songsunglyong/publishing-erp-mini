#!/bin/sh
set -eu
. "$(dirname "$0")/env.sh"
exec ./mvnw clean verify "$@"
