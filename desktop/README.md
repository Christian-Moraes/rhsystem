# RH System Desktop Offline (Tauri)

Este pacote desktop reutiliza o backend Spring Boot e o banco PostgreSQL local.

## Pre-requisitos

- Docker + Docker Compose
- Node.js 20+
- Rust toolchain (para build do Tauri)

## Build do instalador

Na raiz do projeto:

```bash
make desktop-package
```

O comando acima executa:

1. build do backend,
2. build desktop do frontend (`VITE_API_BASE_URL=http://127.0.0.1:8086/api`),
3. build do instalador Tauri.

## Execucao local do stack desktop

```bash
make desktop-start
```

Para encerrar:

```bash
make desktop-stop
```

## Observacoes de offline

- Operacoes principais (auth, empresas, funcionarios, batidas e fechamento) funcionam offline.
- Importacao por imagem com Groq fica desabilitada no perfil `desktop`.
