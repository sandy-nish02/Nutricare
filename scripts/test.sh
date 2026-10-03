#!/usr/bin/env sh
set -eu
PROJECT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
cd "$PROJECT_DIR"
if command -v mvn >/dev/null 2>&1; then
  mvn clean test package
elif [ -x "$PROJECT_DIR/.tools/apache-maven-3.9.9/bin/mvn" ]; then
  "$PROJECT_DIR/.tools/apache-maven-3.9.9/bin/mvn" clean test package
else
  echo "Run ./run.sh once to prepare Maven, then run this test script again."
  exit 1
fi
