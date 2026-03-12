# Portas Configuradas - RH System

## Portas Externas (Host)

Configuradas com +5 acima das portas padrão para evitar conflitos:

- **PostgreSQL**: `5440` (externo) → `5432` (interno)
- **Backend API**: `8086` (externo) → `8080` (interno)
- **Frontend**: `3006` (externo) → `80` (interno)

## Acesso aos Serviços

### Frontend
- **URL**: http://localhost:3006
- Acesse o sistema através desta URL

### Backend API
- **URL**: http://localhost:8086/api
- **Swagger/Health**: http://localhost:8086/actuator (se configurado)

### PostgreSQL (Conexão Externa)
- **Host**: localhost
- **Porta**: 5440
- **Database**: rhsystem
- **Usuário**: postgres
- **Senha**: postgres

## Comunicação Interna (Docker)

Dentro da rede Docker, os serviços se comunicam usando:
- **PostgreSQL**: `postgres:5432`
- **Backend**: `backend:8080`
- **Frontend**: `frontend:80`

O nginx do frontend já está configurado para fazer proxy para `backend:8080` (porta interna).

## Verificar Status

```bash
# Ver containers rodando
docker-compose ps

# Ver logs
docker-compose logs -f

# Ver logs de um serviço específico
docker-compose logs -f backend
docker-compose logs -f frontend
docker-compose logs -f postgres
```

## Conectar ao PostgreSQL

```bash
# Via psql dentro do container
docker-compose exec postgres psql -U postgres -d rhsystem

# Ou via cliente externo usando:
# Host: localhost
# Porta: 5440
```
