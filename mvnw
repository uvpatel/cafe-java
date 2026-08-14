#!/bin/sh
# Minimal Maven Wrapper script for downloading and launching Maven
MAVEN_VER="3.9.9"
DIR="$(cd "$(dirname "$0")" && pwd)"
M2_DIR="$HOME/.m2/wrapper/dists/apache-maven-$MAVEN_VER"

if [ ! -d "$M2_DIR" ]; then
  mkdir -p "$M2_DIR"
  echo "Downloading Maven $MAVEN_VER..."
  curl -sL "https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/$MAVEN_VER/apache-maven-$MAVEN_VER-bin.tar.gz" | tar -xz -C "$M2_DIR"
fi

MVN_BIN="$(find "$M2_DIR" -name mvn -type f | head -n 1)"
exec "$MVN_BIN" "$@"
