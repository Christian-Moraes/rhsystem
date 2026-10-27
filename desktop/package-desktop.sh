#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

echo "[desktop] build backend (jar)..."
docker-compose -f "${ROOT_DIR}/docker-compose.yml" build backend

echo "[desktop] build frontend desktop..."
(cd "${ROOT_DIR}/frontend" && npm run build:desktop)

echo "[desktop] instalando dependencias da CLI Tauri..."
(cd "${ROOT_DIR}/desktop" && npm install)

echo "[desktop] gerando instalador desktop..."
(cd "${ROOT_DIR}/desktop" && npm run tauri:build)

echo "[desktop] concluido."
