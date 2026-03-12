# 🚀 Quick Start - RH System

## Execução Rápida com Docker

```bash
# 1. Subir todos os serviços
docker-compose up -d

# 2. Aguardar inicialização (30-60 segundos)
# Verificar logs:
docker-compose logs -f

# 3. Acessar aplicação
# Frontend: http://localhost:3000
# Backend API: http://localhost:8080/api
```

## Estrutura Criada

### ✅ Backend (Java 21 + Spring Boot)
- ✅ Arquitetura Hexagonal completa
- ✅ Domain-Driven Design (DDD)
- ✅ Entidades de domínio (User, Company, Employee, TimeEvent, etc.)
- ✅ TimeCalculationEngine (cálculo de jornada)
- ✅ Autenticação JWT
- ✅ Migrations Flyway
- ✅ Repositórios e adapters
- ✅ Controllers REST

### ✅ Frontend (React + Vite + TypeScript)
- ✅ Estrutura base React
- ✅ TailwindCSS configurado
- ✅ TanStack Query configurado
- ✅ React Hook Form configurado
- ✅ Contextos (AuthContext, CompanyContext)
- ✅ Páginas básicas (Login, Dashboard)
- ✅ Roteamento configurado

### ✅ Docker
- ✅ Dockerfile backend (multi-stage build)
- ✅ Dockerfile frontend (Nginx)
- ✅ docker-compose.yml completo
- ✅ Health checks configurados
- ✅ Network isolada

## Próximos Passos

1. **Testar Login/Registro**
   - Criar usuário via `/api/auth/register`
   - Fazer login via `/api/auth/login`

2. **Implementar CRUDs**
   - Companies
   - Employees
   - Time Events
   - Timesheets

3. **Adicionar Funcionalidades**
   - Importação de ponto com IA
   - Relatórios
   - Dashboard completo

## Comandos Úteis

```bash
# Ver logs
docker-compose logs -f

# Parar serviços
docker-compose down

# Rebuild
docker-compose build

# Limpar tudo
docker-compose down -v
```

## Arquitetura

```
domain/          # Lógica de negócio pura (sem frameworks)
application/     # Use cases e DTOs
adapters/
  inbound/       # Controllers (REST)
  outbound/      # Persistência (JPA)
config/          # Configurações Spring
shared/          # Utilitários compartilhados
```

## Banco de Dados

- **Event-Based Time Tracking**: Apenas eventos são armazenados
- **Cálculo sob demanda**: TimeCalculationEngine calcula jornadas
- **Cache de fechamento**: Timesheet armazena cálculos mensais

## Segurança

- JWT Authentication
- Spring Security configurado
- CORS configurado
- Validação de dados (Bean Validation)
