#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR"

echo "=== Building keycloak-ydb-extension (core) ==="
mvn -f core/pom.xml clean package -q -DskipTests

JAR_FILE="core/target/keycloak-ydb-extension-1.0-SNAPSHOT.jar"
if [ ! -f "$JAR_FILE" ]; then
    echo "Error: $JAR_FILE not found. Build may have failed."
    exit 1
fi

mkdir -p docker/providers
cp "$JAR_FILE" docker/providers/keycloak-ydb-extension-1.0-SNAPSHOT.jar
echo "  JAR copied to docker/providers/"

echo ""
echo "=== Building retry-proxy ==="
mvn -f retry-proxy/pom.xml package -q -DskipTests
echo "  retry-proxy JAR built"

echo ""
echo "=== Starting Docker Compose (YDB + Keycloak + retry-proxy) ==="
docker compose -f docker/docker-compose.yml up -d --build

echo ""
echo "Stack is starting. Wait ~30-60s for Keycloak to initialize."
echo ""
echo "  Keycloak (via retry-proxy): http://localhost:9090"
echo "  YDB Monitoring:             http://localhost:8765"
echo "  Admin credentials:          admin / admin"
echo ""
echo "Check logs:  docker compose -f docker/docker-compose.yml logs -f ydb-keycloak"
