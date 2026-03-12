flowchart TD

A[Usuário acessa sistema] --> B{Usuário autenticado?}

B -- Não --> C[Tela de Login]
C --> D[Enviar email e senha]
D --> E[Backend valida credenciais]
E --> F{Credenciais válidas?}

F -- Não --> C
F -- Sim --> G[Gerar JWT Token]

G --> H[Retornar usuário + empresas vinculadas]

H --> I[Dashboard]

B -- Sim --> I

I --> J{Selecionar empresa}

J --> K[Company Context ativo]

K --> L[Menu principal]

L --> M[Empresas]
L --> N[Funcionários]
L --> O[Registro de Ponto]
L --> P[Importação de Ponto]
L --> Q[Fechamento]
L --> R[Relatórios]

M --> M1[Criar empresa]
M --> M2[Editar empresa]
M --> M3[Convidar usuário]

N --> N1[Cadastrar funcionário]
N --> N2[Editar funcionário]
N --> N3[Listar funcionários]

O --> O1[Registrar evento de ponto]

O1 --> O2[Salvar TimeEvent no banco]

P --> P1[Upload de arquivo]
P1 --> P2[Backend processa arquivo]
P2 --> P3[IA interpreta registros]
P3 --> P4[Gerar eventos de ponto]

P4 --> O2

O --> O3[Visualizar eventos do funcionário]

O3 --> O4[Backend busca TimeEvents]

O4 --> O5[TimeCalculationEngine]

O5 --> O6[Calcular horas trabalhadas]
O5 --> O7[Calcular horas extras]
O5 --> O8[Calcular atrasos]

O6 --> O9[Retornar resultado para UI]
O7 --> O9
O8 --> O9

Q --> Q1[Selecionar funcionário e período]

Q1 --> Q2[Buscar eventos]

Q2 --> Q3[Executar TimeCalculationEngine]

Q3 --> Q4[Gerar Timesheet]

Q4 --> Q5[Salvar resumo mensal]

Q5 --> Q6[Disponibilizar relatório]

R --> R1[Selecionar filtros]
R1 --> R2[Gerar relatórios]
R2 --> R3[Exportar PDF/Excel]