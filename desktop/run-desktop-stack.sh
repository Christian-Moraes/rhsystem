#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
RUNTIME_DIR="${ROOT_DIR}/desktop/runtime"

mkdir -p "${RUNTIME_DIR}"

if ! command -v docker-compose >/dev/null 2>&1; then
  echo "docker-compose nao encontrado no PATH."
  exit 1
fi

echo "[desktop] subindo postgres + backend local..."
docker rm -f rhsystem-backend >/dev/null 2>&1 || true
docker-compose -f "${ROOT_DIR}/docker-compose.yml" rm -f -s backend >/dev/null 2>&1 || true
SPRING_PROFILES_ACTIVE=desktop GROQ_ENABLED=false \
  docker-compose -f "${ROOT_DIR}/docker-compose.yml" up -d postgres backend >/dev/null

for i in $(seq 1 30); do
  if curl -sS "http://127.0.0.1:8086/api/auth/login" -o /dev/null 2>&1; then
    echo "[desktop] backend pronto em http://127.0.0.1:8086"
    exit 0
  fi
  sleep 1
done

echo "[desktop] timeout aguardando backend subir. Verifique docker-compose logs backend."
exit 1
