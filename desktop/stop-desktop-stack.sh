#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

echo "[desktop] encerrando backend e postgres locais..."
docker-compose -f "${ROOT_DIR}/docker-compose.yml" stop backend postgres >/dev/null 2>&1 || true
