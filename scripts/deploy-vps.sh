#!/bin/bash
# Deploy na VPS compartilhada com o facilcurriculo.
# Builda as imagens localmente (a VPS tem só 1 GB de RAM), envia via ssh e sobe o compose.
# Uso: ./scripts/deploy-vps.sh
set -euo pipefail

SSH_KEY="${SSH_KEY:-$HOME/Downloads/ssh-key-2026-03-01.key}"
SERVER="${SERVER:-ubuntu@144.22.135.240}"
REMOTE_DIR="/home/ubuntu/rhsystem"
SSH="ssh -i $SSH_KEY $SERVER"

cd "$(dirname "$0")/.."

echo "🔨 Build das imagens..."
docker build -t rhsystem-backend:latest ./backend
docker build -t rhsystem-frontend:latest ./frontend

echo "📦 Enviando imagens..."
docker save rhsystem-backend:latest rhsystem-frontend:latest | gzip | $SSH 'gunzip | docker load'

echo "📋 Enviando compose..."
$SSH "mkdir -p $REMOTE_DIR"
scp -i "$SSH_KEY" docker-compose.vps.yml "$SERVER:$REMOTE_DIR/docker-compose.yml"

echo "🚀 Subindo containers..."
$SSH bash -s <<EOF
set -e
cd $REMOTE_DIR
if [ ! -f .env ]; then
  echo "📝 Gerando .env com segredos novos..."
  umask 077
  cat > .env <<ENV
DB_USERNAME=rhsystem
DB_PASSWORD=\$(openssl rand -hex 24)
JWT_SECRET=\$(openssl rand -hex 48)
GROQ_API_KEY=
ENV
fi
docker compose up -d --remove-orphans
docker image prune -f >/dev/null
docker ps --filter name=rhsystem --format 'table {{.Names}}\t{{.Status}}'
EOF

echo "✅ Deploy concluído"
