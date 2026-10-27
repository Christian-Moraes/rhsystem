# RH System

Sistema web de controle de ponto para o Departamento Pessoal. Lê a foto ou o PDF da folha de ponto com IA, monta a grade de batidas para conferência e calcula horas trabalhadas, extras, atrasos e faltas do mês.

Projeto Integrador II da UNIVESP, desenvolvido com a BM Contábil (Campinas/SP).

- **Sistema em produção:** https://rh.144-22-135-240.sslip.io
- **Vídeo de apresentação:** https://youtu.be/EmUcnv0-FZg

## Funcionalidades

- **Empresas e funcionários:** um usuário atende várias empresas; cada funcionário tem sua escala de trabalho.
- **Importação da folha de ponto:** envio de imagem (JPG/PNG) ou PDF; a IA (Groq, modelo de visão) devolve as batidas e o mês da folha.
- **Preparar imagem (LGPD):** antes do envio, o usuário recorta a folha e cobre com tarja nome, CPF e outros dados pessoais. Isso é feito no navegador; a imagem enviada já sai sem esses dados e sem metadados (EXIF).
- **Conferência do período:** se a folha for de um mês diferente do selecionado na tela, o sistema muda para o mês da folha e avisa antes de salvar.
- **Grade de conferência:** batidas editáveis, com destaque para as que a IA marcou como incertas.
- **Cálculo e fechamento mensal:** horas trabalhadas, extras, atrasos e faltas, com exportação para Excel.

## Tecnologias

| Camada | Tecnologia |
|---|---|
| Backend | Java 21, Spring Boot 3.2, arquitetura hexagonal |
| Frontend | React, Vite, TypeScript, TailwindCSS |
| Banco de dados | PostgreSQL 16, migrations com Flyway |
| Autenticação | JWT |
| Leitura da folha | API Groq (modelo de visão) |
| Infraestrutura | Docker Compose; versão desktop com Tauri |

## Estrutura

```
rhsystem/
├── backend/                 # API Spring Boot
├── frontend/                # Interface React
├── desktop/                 # Scripts da versão desktop offline
├── src-tauri/               # Empacotamento desktop (Tauri)
├── scripts/deploy-vps.sh    # Deploy na VPS
├── docker-compose.yml       # Desenvolvimento
├── docker-compose.prod.yml  # Produção genérica
└── docker-compose.vps.yml   # Produção na VPS
```

## Como executar

### Com Docker (recomendado)

Requisitos: Docker e Docker Compose.

```bash
git clone https://github.com/Christian-Moraes/rhsystem.git
cd rhsystem
cp .env.example .env    # preencha GROQ_API_KEY para usar a importação por imagem
make up                 # ou: docker-compose up -d
```

Acesse:

- Frontend: http://localhost:3000
- API: http://localhost:8080/api
- PostgreSQL: localhost:5432

Outros comandos: `make logs`, `make down`, `make clean` (apaga também os dados).

### Sem Docker (desenvolvimento)

```bash
# Backend (Java 21 + Maven)
cd backend && mvn spring-boot:run

# Frontend (Node 20+)
cd frontend && npm install && npm run dev
```

### Versão desktop offline

```bash
make desktop-package   # gera o instalador
make desktop-start     # sobe backend e banco locais
make desktop-stop
```

Na versão desktop a importação por imagem fica desabilitada (sem internet). Detalhes em [desktop/README.md](desktop/README.md).

### Deploy na VPS

```bash
./scripts/deploy-vps.sh
```

O script builda as imagens localmente, envia por SSH e sobe o `docker-compose.vps.yml`. Na primeira execução, gera na VPS um `.env` com senhas aleatórias. A chave da Groq deve ser preenchida nesse `.env`.

## Variáveis de ambiente

| Variável | Uso |
|---|---|
| `GROQ_API_KEY` | Chave da API Groq (https://console.groq.com). Sem ela, a importação por imagem não funciona. |
| `GROQ_MODEL` | Modelo de visão (padrão: `qwen/qwen3.8-27b`) |
| `DB_USERNAME`, `DB_PASSWORD` | Acesso ao PostgreSQL |
| `JWT_SECRET` | Assinatura dos tokens de login |

Nunca versione o arquivo `.env`.

## Proteção de dados (LGPD)

- O recorte e a tarja são feitos no navegador, antes do envio.
- O prompt enviado à IA não contém dados do cadastro do funcionário.
- A imagem não é armazenada no servidor, e os logs não registram nomes de arquivo.
- O acesso é por HTTPS com login (JWT), e cada usuário só vê as empresas a que está vinculado.

## Modelo de dados

O sistema grava apenas os **eventos de ponto** e calcula a jornada sob demanda, o que permite recalcular a qualquer momento.

- `users`, `companies`, `user_companies`: usuários e empresas
- `employees`, `work_schedules`: funcionários e escalas
- `time_events`: batidas (entrada, início e fim do intervalo, saída)
- `timesheets`: fechamento mensal

O cálculo fica no `TimeCalculationEngine`, um serviço de domínio puro, sem dependência de framework ou banco.

## API principal

Todas as rotas ficam sob `/api`. Com exceção do login, exigem o token JWT e o cabeçalho `X-Company-Id` com a empresa ativa.

| Rota | Função |
|---|---|
| `POST /auth/login`, `POST /auth/register` | Login e cadastro |
| `GET/POST /companies` | Empresas |
| `GET/POST /companies/{id}/employees` | Funcionários |
| `GET /companies/{id}/time-events/employee/{emp}/month/{m}/year/{a}` | Batidas do mês |
| `POST /companies/{id}/time-import/employee/{emp}` | Importação da folha (imagem ou PDF) |
| `POST /companies/{id}/timesheets/employee/{emp}/month/{m}/year/{a}` | Fechamento do mês |
| `GET /companies/{id}/timesheets/employee/{emp}/month/{m}/year/{a}/export` | Exportação para Excel |
| `GET/POST /companies/{id}/work-schedules` | Escalas de trabalho |

## Licença

Projeto acadêmico. Uso restrito aos autores e à BM Contábil.
