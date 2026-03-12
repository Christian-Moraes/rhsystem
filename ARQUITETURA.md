# Arquitetura do Sistema - RH System

## 🎯 Visão Geral

Sistema SaaS de gestão de ponto para **DP/RH** e **Escritórios de Contabilidade** que gerenciam múltiplas empresas.

## 🏗️ Arquitetura Hexagonal (Ports & Adapters)

```
┌─────────────────────────────────────────────────┐
│              ADAPTERS (INBOUND)                 │
│         Controllers REST (HTTP)                │
└───────────────────┬─────────────────────────────┘
                    │
┌───────────────────▼─────────────────────────────┐
│            APPLICATION LAYER                      │
│  Use Cases | DTOs | Services de Aplicação        │
└───────────────────┬─────────────────────────────┘
                    │
┌───────────────────▼─────────────────────────────┐
│              DOMAIN LAYER                        │
│  Entities | Value Objects | Domain Services      │
│  Repositories (Interfaces)                      │
│  ✅ TimeCalculationEngine (puro domínio)        │
└───────────────────┬─────────────────────────────┘
                    │
┌───────────────────▼─────────────────────────────┐
│           ADAPTERS (OUTBOUND)                    │
│  JPA Entities | Repository Implementations      │
│  External Services (IA, Email, etc.)           │
└─────────────────────────────────────────────────┘
```

## 📊 Modelo de Dados

### Entidades Principais

#### User
- Informações de autenticação
- Pode ter múltiplas empresas

#### Company
- Dados da empresa
- **Configurações de ponto**:
  - Hora extra (habilitada/desabilitada)
  - Banco de horas (habilitado/desabilitado)
  - Porcentagem hora extra
  - Porcentagem fim de semana

#### UserCompany
- Relacionamento usuário-empresa
- Roles: ADMIN, MANAGER, VIEWER

#### Employee
- Funcionário vinculado a uma empresa
- Escala de trabalho (pode herdar da empresa)

#### WorkSchedule
- Configuração de jornada
- Horários de entrada/saída
- Intervalos
- Tolerâncias

#### TimeEvent (Event-Based)
- Eventos de ponto
- Tipos: ENTRY, BREAK_START, BREAK_END, EXIT, MANUAL_EDIT, IMPORT
- **Status**: CONFIRMED, REVIEW, UNCERTAIN
- Fonte: MANUAL, IMPORT, etc.

#### Timesheet
- Cache de fechamento mensal
- Resultados calculados
- Pode ser recalculado

## 🔄 Fluxos Principais

### 1. Registro de Usuário
```
Registro → Cria User → Cria Company → Associa UserCompany (ADMIN) → Cria WorkSchedule padrão
```

### 2. Configuração de Empresa
```
Empresa → Configura Hora Extra → Configura Banco → Define Porcentagens → Salva
```

### 3. Cadastro de Funcionário
```
Funcionário → Vincula a Company → Define Escala → Ativa
```

### 4. Registro de Ponto
```
Upload (IA) → Processa → Cria Eventos (REVIEW/UNCERTAIN) → DP Revisa → Confirma → Salva
```

### 5. Fechamento Mensal
```
Seleciona Mês → Busca Eventos → TimeCalculationEngine Calcula → Salva Timesheet → Gera Relatório
```

## 🧮 TimeCalculationEngine

### Entrada
- Lista de `TimeEvent`
- `WorkSchedule`
- `Company` (configurações)

### Processamento
1. Agrupa eventos por data
2. Para cada dia:
   - Calcula horas trabalhadas
   - Calcula atrasos
   - Calcula horas extras (se habilitado)
   - Considera banco de horas (se habilitado)
   - Aplica porcentagens (fim de semana, hora extra)

### Saída
- `TimeCalculationResult`:
  - Horas trabalhadas
  - Horas extras
  - Atrasos
  - Ausências

## 🔐 Autenticação

### JWT
- Access Token (24h)
- Refresh Token (7 dias)
- Claims: userId, companyId, role

### Contexto
- `UserContext` contém userId, companyId, role
- Header `X-Company-Id` para alternar empresa

## 📱 Frontend

### Estrutura
```
src/
├── app/          # Configuração de rotas
├── components/   # Componentes reutilizáveis
├── contexts/    # AuthContext, CompanyContext
├── features/     # Módulos por funcionalidade
├── hooks/        # Custom hooks
├── pages/        # Páginas principais
└── services/     # API clients
```

### Contextos
- **AuthContext**: Autenticação e usuário
- **CompanyContext**: Empresa ativa e lista de empresas

## 🚀 Próximos Passos

1. Implementar CRUD de Empresas (com configurações)
2. Implementar CRUD de Funcionários
3. Implementar registro manual de ponto
4. Integrar IA para processamento de arquivos
5. Implementar dashboard com gráficos
6. Implementar fechamento mensal
7. Implementar recuperação de senha
