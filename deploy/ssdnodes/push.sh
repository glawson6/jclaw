#!/usr/bin/env bash
# push.sh — Build Docker image and push to private registry
# Run from the JClaw repo root on your dev machine
# Usage: ./deploy/ssdnodes/push.sh [version]
set -euo pipefail

REGISTRY="tooling.taptech.net:8082"
IMAGE_PREFIX="io.jclaw"
VERSION="${1:-0.1.0-SNAPSHOT}"

REPO_ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
cd "${REPO_ROOT}"

export JAVA_HOME="${JAVA_HOME:-/Users/tap/.sdkman/candidates/java/21.0.9-oracle}"

echo "==> Building Docker image with JKube"
./mvnw package k8s:build \
    -pl :jclaw-gateway-app -am \
    -Pk8s -DskipTests

GATEWAY_IMAGE="${IMAGE_PREFIX}/jclaw-gateway-app:${VERSION}"

echo "==> Tagging for registry"
docker tag "${GATEWAY_IMAGE}" "${REGISTRY}/${GATEWAY_IMAGE}"

echo "==> Pushing to ${REGISTRY}"
docker push "${REGISTRY}/${GATEWAY_IMAGE}"

echo ""
echo "==> Pushed ${REGISTRY}/${GATEWAY_IMAGE}"
echo ""
echo "Deploy on VM with:"
echo "  ssh jclaw@<vm-ip> '/opt/jclaw/deploy.sh ${VERSION}'"
echo ""
echo "Or wait for Watchtower to pick it up (~5 min)."
