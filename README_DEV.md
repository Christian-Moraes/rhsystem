# Modo Desenvolvimento e Produção - RH System

## 🚀 Desenvolvimento (Padrão)

O `docker-compose.yml` padrão está configurado para **desenvolvimento** com hot reload:

```bash
# Subir em modo desenvolvimento
docker-compose up -d
# ou
make up

# Ver logs
docker-compose logs -f frontend
# ou
make logs
```

### Recursos do Modo Dev

- ✅ **Hot reload automático** - Mudanças refletem imediatamente
- ✅ **Sem rebuild** - Não precisa reconstruir a cada mudança
- ✅ **Volumes montados** - Código sincronizado entre host e container
- ✅ **Vite dev server** - HMR (Hot Module Replacement) ativo

## 📦 Produção

Use o `docker-compose.prod.yml` para produção:

```bash
# Subir em modo produção
docker-compose -f docker-compose.prod.yml up -d
# ou
make prod

# Ver logs
docker-compose -f docker-compose.prod.yml logs -f
# ou
make prod-logs
```

### Características do Modo Prod

- ✅ **Build otimizado** - Código minificado e otimizado
- ✅ **Nginx** - Servidor web otimizado para produção
- ✅ **Sem volumes** - Código dentro da imagem
- ✅ **Restart always** - Reinício automático em caso de falha

## 📊 Comandos Úteis

### Desenvolvimento

```bash
make up          # Subir serviços
make down        # Parar serviços
make logs        # Ver logs
make build       # Rebuild imagens
make ps          # Ver status
```

### Produção

```bash
make prod        # Subir em produção
make prod-down   # Parar produção
make prod-logs   # Ver logs produção
make prod-build  # Rebuild produção
```

## 🌐 Acesso

### Desenvolvimento
- **Frontend**: http://localhost:3006 (com hot reload)
- **Backend API**: http://localhost:8086/api

### Produção
- **Frontend**: http://localhost:3006
- **Backend API**: http://localhost:8086/api

## ⚠️ Notas

- **Desenvolvimento**: Use `docker-compose.yml` (padrão)
- **Produção**: Use `docker-compose.prod.yml`
- O modo dev consome mais recursos (não usar em produção)
- Em produção, configure variáveis de ambiente no `.env`
