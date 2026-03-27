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

JAVA_VER_LINE="$(java -version 2>&1 || true)"
JAVA_VER="$(echo "$JAVA_VER_LINE" | awk -F\" '/version/ {print $2}' || true)"
JAVA_MAJOR="${JAVA_VER%%.*}"

if [ -z "$JAVA_MAJOR" ]; then
  echo "Erro: não consegui detectar a versão do Java (java -version)."
  exit 1
fi

if [ "$JAVA_MAJOR" != "21" ]; then
  echo "Aviso: seu Java atual é $JAVA_MAJOR (precisa ser 21)."
  # Tenta achar um JDK 21 instalado e usar automaticamente
  if [ -z "${JAVA_HOME:-}" ]; then
    if [ -d /usr/lib/jvm/java-21-openjdk-amd64 ]; then
      export JAVA_HOME="/usr/lib/jvm/java-21-openjdk-amd64"
      export PATH="$JAVA_HOME/bin:$PATH"
      echo "Tentando usar JAVA_HOME=$JAVA_HOME"
    else
      JAVA21_HOME="$(ls -d /usr/lib/jvm/java-21-openjdk* 2>/dev/null | head -n 1 || true)"
      if [ -n "$JAVA21_HOME" ]; then
        export JAVA_HOME="$JAVA21_HOME"
        export PATH="$JAVA_HOME/bin:$PATH"
        echo "Tentando usar JAVA_HOME=$JAVA_HOME"
      else
        echo "Instale o JDK 21 para rodar em dev sem buildar Docker."
        echo "Exemplo: sudo apt-get update && sudo apt-get install -y openjdk-21-jdk"
        exit 1
      fi
    fi
  fi
fi

echo "Java em uso:"
java -version

echo "Backend em modo dev (porta 8086). Postgres: localhost:5440. Ctrl+C para parar."
mvn -q spring-boot:run -Dspring-boot.run.profiles=dev
