#!/usr/bin/env bash
# Roda o backend em modo desenvolvimento (sem buildar Docker).
# Equivalente ao "npm run dev" do React: sobe o app; com DevTools, recompilar faz reiniciar.
#
# Pré-requisito: Postgres no ar. Ex.: na raiz do projeto: docker-compose up -d postgres
#
# Uso: ./run-dev.sh   ou   bash run-dev.sh

set -e
cd "$(dirname "$0")"

if [ -f ../.env ]; then
  set -a
  source ../.env
  set +a
fi

export SPRING_DATASOURCE_URL="${SPRING_DATASOURCE_URL:-jdbc:postgresql://localhost:5440/rhsystem}"
export DB_USERNAME="${DB_USERNAME:-postgres}"
export DB_PASSWORD="${DB_PASSWORD:-postgres}"
export JWT_SECRET="${JWT_SECRET:-your-256-bit-secret-key-change-in-production-minimum-32-characters}"
export GROQ_API_KEY="${GROQ_API_KEY:-}"

echo "Backend em modo dev (porta 8086). Postgres: localhost:5440. Ctrl+C para parar."
mvn -q spring-boot:run -Dspring-boot.run.profiles=dev
