# Guia Docker - RH System

## 🐳 Execução Rápida

### Opção 1: Usando Makefile

```bash
# Subir todos os serviços
make up

# Ver logs
make logs

# Parar serviços
make down

# Rebuild e subir
make build && make up

# Limpar tudo (cuidado: remove volumes)
make clean
```

### Opção 2: Usando Docker Compose diretamente

```bash
# Subir todos os serviços
docker-compose up -d

# Ver logs
docker-compose logs -f

# Parar serviços
docker-compose down

# Rebuild
docker-compose build

# Ver status
docker-compose ps
```

## 📋 Serviços

### PostgreSQL
- **Porta**: 5432
- **Database**: rhsystem
- **Usuário**: postgres
- **Senha**: postgres (padrão)

### Backend (Spring Boot)
- **Porta**: 8080
- **URL**: http://localhost:8080
- **API Base**: http://localhost:8080/api

### Frontend (React + Nginx)
- **Porta**: 3000
- **URL**: http://localhost:3000

## 🔧 Variáveis de Ambiente

Crie um arquivo `.env` na raiz do projeto (opcional):

```env
DB_USERNAME=postgres
DB_PASSWORD=postgres
JWT_SECRET=sua-chave-secreta-aqui-minimo-32-caracteres
```

## 🚀 Primeira Execução

1. Clone o repositório
2. Execute `docker-compose up -d`
3. Aguarde os serviços iniciarem (pode levar alguns minutos na primeira vez)
4. Acesse http://localhost:3000

## 🔍 Troubleshooting

### Ver logs de um serviço específico

```bash
docker-compose logs -f backend
docker-compose logs -f frontend
docker-compose logs -f postgres
```

### Reiniciar um serviço específico

```bash
docker-compose restart backend
docker-compose restart frontend
```

### Acessar shell do container

```bash
docker exec -it rhsystem-backend sh
docker exec -it rhsystem-postgres psql -U postgres -d rhsystem
```

### Limpar e recriar

```bash
docker-compose down -v
docker-compose up -d --build
```

## 📊 Health Checks

O PostgreSQL possui health check configurado. O backend aguarda o banco estar pronto antes de iniciar.

## 🔐 Segurança

⚠️ **IMPORTANTE**: Em produção, altere:
- Senha do PostgreSQL
- JWT_SECRET
- Credenciais padrão
