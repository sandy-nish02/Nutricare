#!/usr/bin/env sh
set -eu

PROJECT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
MAVEN_VERSION=3.9.9
LOCAL_MAVEN="$PROJECT_DIR/.tools/apache-maven-$MAVEN_VERSION/bin/mvn"

if ! command -v java >/dev/null 2>&1; then
  echo "Java 17 or newer is required. Install a JDK from https://adoptium.net/"
  exit 1
fi

JAVA_MAJOR=$(java -version 2>&1 | awk -F '[".]' '/version/ {print $2; exit}')
if [ "${JAVA_MAJOR:-0}" -lt 17 ]; then
  echo "NutriCare requires Java 17 or newer. Detected Java $JAVA_MAJOR."
  exit 1
fi

if command -v mvn >/dev/null 2>&1; then
  MAVEN_CMD=mvn
elif [ -x "$LOCAL_MAVEN" ]; then
  MAVEN_CMD=$LOCAL_MAVEN
else
  mkdir -p "$PROJECT_DIR/.tools"
  ARCHIVE="$PROJECT_DIR/.tools/maven.tar.gz"
  URL="https://archive.apache.org/dist/maven/maven-3/$MAVEN_VERSION/binaries/apache-maven-$MAVEN_VERSION-bin.tar.gz"
  echo "Maven is not installed. Downloading a private project copy once..."
  if command -v curl >/dev/null 2>&1; then
    curl -fL "$URL" -o "$ARCHIVE"
  elif command -v wget >/dev/null 2>&1; then
    wget -O "$ARCHIVE" "$URL"
  else
    echo "Install Maven 3.9+ or install curl/wget, then run this file again."
    exit 1
  fi
  tar -xzf "$ARCHIVE" -C "$PROJECT_DIR/.tools"
  rm -f "$ARCHIVE"
  MAVEN_CMD=$LOCAL_MAVEN
fi

cd "$PROJECT_DIR"
if [ "${DB_MODE:-sqlserver}" = "sqlserver" ] && [ -z "${DB_PASSWORD:-}" ]; then
  echo "Windows authentication is only available on Windows. Set DB_USER and DB_PASSWORD"
  echo "for a SQL Server login (and DB_URL if the server is not on localhost:1433), then run again."
  exit 1
fi
echo "Starting NutriCare at http://localhost:8080/nutricare/"
exec "$MAVEN_CMD" -q jetty:run
