#!/usr/bin/env bash
#
# Downloads keycloak-benchmark from GitHub releases and extracts JARs into lib/.
#
# Usage: ./prepare.sh [version]
#   version defaults to 999.0.0-SNAPSHOT
#
# Examples:
#   ./prepare.sh
#   ./prepare.sh 26.4.0-SNAPSHOT
#

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
LIB_DIR="$SCRIPT_DIR/lib"
VERSION="${1:-999.0.0-SNAPSHOT}"
ARCHIVE="keycloak-benchmark-${VERSION}.tar.gz"
URL="https://github.com/keycloak/keycloak-benchmark/releases/download/${VERSION}/${ARCHIVE}"

mkdir -p "$LIB_DIR"

echo "Downloading keycloak-benchmark ${VERSION} ..."
echo "  URL: $URL"

TMP_DIR=$(mktemp -d)
trap "rm -rf $TMP_DIR" EXIT

curl -fSL --progress-bar -o "$TMP_DIR/$ARCHIVE" "$URL"

echo "Extracting ..."
tar -xzf "$TMP_DIR/$ARCHIVE" -C "$TMP_DIR"

DIST_DIR="$TMP_DIR/keycloak-benchmark-${VERSION}"
if [ ! -d "$DIST_DIR/lib" ]; then
    echo "ERROR: Unexpected archive structure, lib/ not found in $DIST_DIR"
    ls "$TMP_DIR"
    exit 1
fi

rm -f "$LIB_DIR"/*.jar
cp "$DIST_DIR/lib/"*.jar "$LIB_DIR/"

echo
echo "Done. JARs in $LIB_DIR/:"
ls -lh "$LIB_DIR/"
