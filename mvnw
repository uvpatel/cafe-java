#!/bin/sh
set -eu

# Minimal Maven wrapper script for downloading and launching Maven.
MAVEN_VER="3.9.9"
MAVEN_CACHE_ROOT="${MAVEN_USER_HOME:-${HOME}/.m2}"
M2_DIR="${MAVEN_CACHE_ROOT}/wrapper/dists/apache-maven-${MAVEN_VER}"

if [ ! -d "${M2_DIR}" ]; then
  mkdir -p "${M2_DIR}"
  echo "Downloading Maven ${MAVEN_VER}..."
  curl --fail --silent --show-error --location     "https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/${MAVEN_VER}/apache-maven-${MAVEN_VER}-bin.tar.gz"     | tar -xz -C "${M2_DIR}"
fi

MVN_BIN="$(find "${M2_DIR}" -name mvn -type f | head -n 1)"
if [ -z "${MVN_BIN}" ]; then
  echo "Unable to locate Maven executable under ${M2_DIR}" >&2
  exit 1
fi

exec "${MVN_BIN}" "$@"
