# RH System - Requisitos e Arquitetura

## 🎯 Visão Geral

Sistema B2B de gestão de ponto para **Departamento Pessoal / RH** que gerencia múltiplas empresas clientes.

## 👥 Perfil de Usuário

### DP/RH
- Gerencia múltiplas empresas clientes
- Um login para acessar todas as empresas
- Cadastra empresas e configura regras
- Vincula funcionários às empresas
- Processa ponto através de imagens, PDF ou CSV usando IA

## 🔐 Autenticação

### Funcionalidades
- ✅ **Login** - Autenticação com email/senha
- ✅ **Registro** - Criação de conta (cria empresa padrão automaticamente)
- ⏳ **Esqueci a Senha** - Solicitação de recuperação
- ⏳ **Recuperação/Trocar Senha** - Reset de senha via token

### Fluxo de Registro
1. DP/RH se registra com nome, email e senha
2. Sistema cria apenas o usuário
3. DP/RH pode criar empresas e configurá-las após o login

## 🏢 Cadastro de Empresas

### Informações Básicas
- Nome da empresa
- CNPJ
- Responsável
- Email
- Telefone

### Configurações de Ponto
- ✅ **Hora Extra**: Habilitar/desabilitar
- ✅ **Banco de Horas**: Habilitar/desabilitar
- ✅ **Porcentagem Hora Extra**: % sobre hora normal (ex: 50%, 100%)
- ✅ **Porcentagem Fim de Semana**: % sobre hora normal (ex: 50%, 100%)
- ✅ **Escala de Trabalho**: Horário de entrada/saída padrão
- ✅ **Tolerância de Atraso**: Minutos tolerados
- ✅ **Tolerância de Saída Antecipada**: Minutos tolerados
- ✅ **Intervalo de Almoço**: Minutos de intervalo

## 👤 Cadastro de Funcionários

### Informações
- Nome completo
- CPF
- Cargo/Função
- Data de admissão
- Empresa vinculada
- Escala de trabalho (pode herdar da empresa ou ter escala própria)
- Status (Ativo, Inativo, Afastado)

## ⏰ Registro de Ponto

### Eventos de Ponto (Event-Based)
- **ENTRY** - Entrada
- **BREAK_START** - Início do intervalo
- **BREAK_END** - Fim do intervalo
- **EXIT** - Saída
- **MANUAL_EDIT** - Edição manual pelo DP/RH
- **IMPORT** - Importado via IA

### Processamento com IA
1. DP/RH faz upload de:
   - **Imagem** (foto do ponto físico)
   - **CSV** (planilha de ponto)
   - **PDF** (folha de ponto digitalizada)

2. IA analisa e extrai:
   - Datas
   - Horários de entrada/saída
   - Padrão: entrada → saída → entrada → saída

3. Sistema cria eventos de ponto automaticamente com status:
   - **CONFIRMED** - Confiança alta
   - **REVIEW** - Requer revisão manual
   - **UNCERTAIN** - Baixa confiança

4. DP/RH revisa e ajusta:
   - Edita eventos criados
   - Confirma ou rejeita
   - Ajusta horários
   - Adiciona eventos faltantes

### Edição Manual
- Adicionar eventos de ponto
- Editar horários existentes
- Remover eventos incorretos
- Justificar ausências/atrasos

## 📊 Dashboard Inicial

### Gráficos e Métricas
- Total de funcionários ativos
- Total de empresas gerenciadas
- Horas trabalhadas no mês (agregado)
- Horas extras no mês
- Atrasos no mês
- Ausências no mês
- Gráfico de ponto por dia da semana
- Gráfico de horas trabalhadas por empresa
- Alertas de pontos pendentes de revisão

## 📅 Fechamento Mensal

### Processo
1. DP/RH seleciona empresa e mês/ano
2. Sistema processa todos os eventos do período
3. **TimeCalculationEngine** calcula:
   - Horas trabalhadas
   - Horas extras
   - Atrasos
   - Ausências
   - Banco de horas (se habilitado)

4. Resultado é salvo em **Timesheet** (cache)
5. Pode ser recalculado a qualquer momento
6. Geração de relatórios:
   - PDF
   - Excel
   - Visualização na tela

### Regras de Cálculo
- Considera escala de trabalho do funcionário
- Aplica tolerâncias configuradas
- Calcula horas extras conforme porcentagem configurada
- Considera banco de horas se habilitado
- Aplica regras de fim de semana se configurado

## 🏗️ Arquitetura Técnica

### Backend
- **Java 21** + Spring Boot
- **Arquitetura Hexagonal** (Ports & Adapters)
- **DDD** (Domain-Driven Design)
- **PostgreSQL** com Flyway
- **JWT** Authentication

### Frontend
- **React** + Vite + TypeScript
- **TailwindCSS**
- **TanStack Query**
- **React Hook Form**

### Infraestrutura
- **Docker** + Docker Compose
- Modo desenvolvimento com hot reload
- Modo produção otimizado

## 📋 Fluxo Principal

```
1. DP/RH se registra
   ↓
2. DP/RH faz login
   ↓
3. DP/RH cadastra empresas e configura regras (hora extra, banco, etc.)
   ↓
4. DP/RH vincula funcionários às empresas
   ↓
5. DP/RH faz upload de ponto (imagem/PDF/CSV)
   ↓
6. IA analisa e preenche entrada/saída automaticamente
   ↓
7. DP/RH revisa e ajusta pontos se necessário
   ↓
8. Fecha mês (cálculo automático)
   ↓
9. Visualiza relatórios e exporta
```

## 🔄 Próximas Implementações

### Fase 1 - Base (Atual)
- ✅ Autenticação (login/registro)
- ✅ Estrutura de empresas
- ✅ Estrutura de funcionários
- ✅ Eventos de ponto
- ✅ Engine de cálculo

### Fase 2 - Configurações
- ⏳ Tela de configuração de empresa
- ⏳ Parâmetros de hora extra/banco
- ⏳ Escalas de trabalho customizadas

### Fase 3 - Ponto
- ⏳ Tela de registro manual de ponto
- ⏳ Upload de arquivos (imagem/CSV/PDF)
- ⏳ Integração com IA para processamento
- ⏳ Tela de revisão e edição

### Fase 4 - Dashboard e Relatórios
- ⏳ Dashboard com gráficos
- ⏳ Fechamento mensal
- ⏳ Geração de relatórios (PDF/Excel)

### Fase 5 - Recuperação de Senha
- ⏳ Esqueci a senha
- ⏳ Email de recuperação
- ⏳ Trocar senha
