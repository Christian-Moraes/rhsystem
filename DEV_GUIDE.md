# Guia de Desenvolvimento

## Frontend em Modo Dev

O frontend está configurado para rodar em **modo desenvolvimento** com hot reload automático.

### Quando NÃO precisa rebuildar:

✅ **Mudanças em arquivos de código** (`src/**/*`)
- O Vite detecta automaticamente e recarrega
- Hot Module Replacement (HMR) funciona automaticamente

✅ **Mudanças em configurações** (vite.config.ts, tailwind.config.js, etc.)
- Arquivos são montados como volumes, mudanças são refletidas

### Quando PRECISA rebuildar:

❌ **Mudanças em `package.json`** (adicionar/remover dependências)
```bash
docker-compose build frontend
docker-compose up -d frontend
```

❌ **Mudanças no `Dockerfile.dev`**
```bash
docker-compose build frontend
docker-compose up -d frontend
```

### Comandos Úteis

**Ver logs do frontend:**
```bash
docker-compose logs -f frontend
```

**Reiniciar apenas o frontend (sem rebuild):**
```bash
docker-compose restart frontend
```

**Rebuild completo (apenas se necessário):**
```bash
docker-compose build frontend
docker-compose up -d frontend
```

**Rebuild tudo:**
```bash
docker-compose down
docker-compose up -d --build
```

## Verificando se está em modo dev

Os logs devem mostrar:
```
> vite --host 0.0.0.0 --port 3000
VITE v5.x.x  ready in XXX ms
➜  Local:   http://localhost:3000/
```

Se estiver mostrando `nginx` ou algo sobre build, está usando o Dockerfile de produção por engano.

## Acesso

- Frontend: http://localhost:3006
- Backend: http://localhost:8086
- PostgreSQL: localhost:5440
