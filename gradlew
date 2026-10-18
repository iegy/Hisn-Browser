#!/usr/bin/env sh
# Gradle bootstrap launcher for HISN Browser.
# The official Gradle Wrapper JAR is intentionally not vendored in this bundle.
# This script downloads the pinned official Gradle distribution and runs it.
set -eu

APP_HOME=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
GRADLE_VERSION="9.3.1"
GRADLE_USER_HOME="${GRADLE_USER_HOME:-$HOME/.gradle}"
DIST_DIR="$GRADLE_USER_HOME/hisn-bootstrap/gradle-$GRADLE_VERSION"
GRADLE_BIN="$DIST_DIR/bin/gradle"
ZIP="$GRADLE_USER_HOME/hisn-bootstrap/gradle-$GRADLE_VERSION-bin.zip"

if [ ! -x "$GRADLE_BIN" ]; then
  mkdir -p "$GRADLE_USER_HOME/hisn-bootstrap"
  if [ ! -f "$ZIP" ]; then
    echo "Downloading official Gradle $GRADLE_VERSION distribution..."
    if command -v curl >/dev/null 2>&1; then
      curl --fail --location --retry 3 --output "$ZIP.part" "https://services.gradle.org/distributions/gradle-$GRADLE_VERSION-bin.zip"
      mv "$ZIP.part" "$ZIP"
    elif command -v wget >/dev/null 2>&1; then
      wget -O "$ZIP.part" "https://services.gradle.org/distributions/gradle-$GRADLE_VERSION-bin.zip"
      mv "$ZIP.part" "$ZIP"
    else
      echo "Error: curl or wget is required to download Gradle." >&2
      exit 1
    fi
  fi
  command -v unzip >/dev/null 2>&1 || { echo "Error: unzip is required to unpack Gradle." >&2; exit 1; }
  unzip -q -o "$ZIP" -d "$GRADLE_USER_HOME/hisn-bootstrap"
fi
exec "$GRADLE_BIN" -p "$APP_HOME" "$@"
