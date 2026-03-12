# RH System - SaaS de Gestão de Ponto

Sistema completo de gestão de ponto para RH / Departamento Pessoal, desenvolvido com arquitetura hexagonal e preparado para escalabilidade.

## 🏗️ Arquitetura

- **Backend**: Java 21 + Spring Boot 3.2
- **Frontend**: React + Vite + TypeScript + TailwindCSS
- **Banco de Dados**: PostgreSQL
- **Autenticação**: JWT
- **Migrations**: Flyway
- **Containerização**: Docker + Docker Compose

## 📁 Estrutura do Projeto

```
rhsystem/
├── backend/          # Backend Spring Boot
├── frontend/         # Frontend React
└── docker-compose.yml
```

## 🚀 Como Executar

### Pré-requisitos

- Docker e Docker Compose instalados
- Maven (para desenvolvimento local)

### Executar com Docker

1. Clone o repositório:
```bash
git clone <repo-url>
cd rhsystem
```

2. Configure as variáveis de ambiente (opcional):
```bash
cp .env.example .env
# Edite o .env conforme necessário
```

3. Execute o docker-compose:
```bash
docker-compose up -d
```

4. Acesse:
- Frontend: http://localhost:3000
- Backend API: http://localhost:8080
- PostgreSQL: localhost:5432

### Desenvolvimento Local

#### Backend

```bash
cd backend
mvn spring-boot:run
```

#### Frontend

```bash
cd frontend
npm install
npm run dev
```

## 📊 Banco de Dados

O sistema utiliza **Event-Based Time Tracking**, armazenando apenas eventos de ponto e calculando jornadas sob demanda. Isso garante:

- Menos escrita no banco
- Cálculos recalculáveis
- Alta escalabilidade
- Menos inconsistências

### Principais Tabelas

- `users` - Usuários do sistema
- `companies` - Empresas clientes
- `user_companies` - Relacionamento usuário-empresa
- `employees` - Funcionários
- `work_schedules` - Escalas de trabalho
- `time_events` - Eventos de ponto (ENTRY, BREAK_START, BREAK_END, EXIT)
- `timesheets` - Fechamento mensal (cache de cálculo)

## 🔐 Autenticação

O sistema utiliza JWT com suporte a múltiplas empresas por usuário. O contexto da empresa ativa pode ser enviado via header:

```
X-Company-Id: <uuid-da-empresa>
```

## 📝 APIs Principais

### Autenticação
- `POST /api/auth/login` - Login
- `POST /api/auth/register` - Registro

### Empresas
- `GET /api/companies` - Listar empresas
- `POST /api/companies` - Criar empresa

### Funcionários
- `GET /api/employees` - Listar funcionários
- `POST /api/employees` - Criar funcionário

### Eventos de Ponto
- `POST /api/time-events` - Registrar evento
- `GET /api/time-events` - Listar eventos

### Fechamento Mensal
- `POST /api/timesheets/close` - Fechar mês
- `GET /api/timesheets` - Listar fechamentos

## 🧮 Engine de Cálculo

O `TimeCalculationEngine` é um serviço de domínio puro que:

- Recebe lista de eventos e escala de trabalho
- Calcula horas trabalhadas, extras, atrasos e ausências
- Não depende de frameworks ou banco de dados
- Pode ser testado isoladamente

## 🎯 Próximos Passos

- [ ] Implementar importação de ponto com IA
- [ ] Adicionar relatórios avançados
- [ ] Implementar notificações
- [ ] Adicionar testes automatizados
- [ ] Configurar CI/CD

## 📄 Licença

Este projeto é proprietário.
