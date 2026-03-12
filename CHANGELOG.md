# Changelog - RH System

## [1.0.0] - 2026-03-11

### ✅ Implementado

#### Backend
- ✅ Arquitetura Hexagonal completa
- ✅ Domain-Driven Design
- ✅ Entidades de domínio (User, Company, Employee, TimeEvent, WorkSchedule, Timesheet)
- ✅ TimeCalculationEngine (cálculo de jornada puro)
- ✅ Autenticação JWT
- ✅ Migrations Flyway
- ✅ Repositórios e adapters
- ✅ Registro de usuário cria empresa automaticamente
- ✅ Configurações de empresa (hora extra, banco de horas, porcentagens)
- ✅ Status de eventos de ponto (CONFIRMED, REVIEW, UNCERTAIN)

#### Frontend
- ✅ Estrutura React + Vite + TypeScript
- ✅ TailwindCSS configurado
- ✅ TanStack Query
- ✅ React Hook Form
- ✅ Contextos (AuthContext, CompanyContext)
- ✅ Páginas: Login, Register, Dashboard básico
- ✅ Roteamento configurado

#### Docker
- ✅ Docker Compose para desenvolvimento (hot reload)
- ✅ Docker Compose para produção
- ✅ PostgreSQL configurado
- ✅ Portas configuradas (5440, 8086, 3006)

### 📝 Documentação
- ✅ REQUISITOS.md - Requisitos completos do sistema
- ✅ ARQUITETURA.md - Arquitetura técnica
- ✅ REFATORACAO.md - Mudanças realizadas
- ✅ README.md - Guia principal
- ✅ QUICKSTART.md - Início rápido
- ✅ DOCKER.md - Guia Docker

### 🔄 Próximas Implementações

#### Fase 2 - Configurações
- ⏳ Tela de configuração de empresa
- ⏳ CRUD completo de empresas
- ⏳ Validação de CNPJ

#### Fase 3 - Funcionários
- ⏳ CRUD de funcionários
- ⏳ Vinculação a empresas
- ⏳ Escalas customizadas por funcionário

#### Fase 4 - Ponto
- ⏳ Registro manual de ponto
- ⏳ Upload de arquivos (imagem/CSV/PDF)
- ⏳ Integração com IA
- ⏳ Tela de revisão de pontos

#### Fase 5 - Dashboard e Relatórios
- ⏳ Dashboard com gráficos
- ⏳ Fechamento mensal
- ⏳ Exportação PDF/Excel

#### Fase 6 - Recuperação de Senha
- ⏳ Esqueci a senha
- ⏳ Email de recuperação
- ⏳ Trocar senha
