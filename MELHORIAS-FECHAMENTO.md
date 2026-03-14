# Melhorias – Fechamento de Ponto

## 1. Atrasos (246h 40m) – Por que aparece e como corrigir

### Causa
O valor **246h 40m em Atrasos** no fechamento de janeiro/2026 está **incorreto**. Acontece quando:

- A **escala de trabalho** (work schedule) da empresa ou do funcionário está com **horário de entrada em 00:00** (meia-noite), por configuração errada ou dado antigo.
- O sistema interpreta que o esperado é entrar à meia-noite. Aí, em todo dia em que a primeira batida (E1) é **13:58**, o atraso vira **~14h** naquele dia.
- Com **22 dias** com ponto, isso gera centenas de horas de “atraso” (ex.: 22 × ~14h ≈ 246h).

Ou seja: o número alto vem da **escala com entrada em 00:00**, não das batidas em si.

### O que já está corrigido no backend
No `TimeCalculationEngine` já existe regra de proteção:

- Se o **horário de entrada** da escala for **antes das 05:00**, o atraso **não é calculado** (considerado 0 naquele dia).
- Assim, escalas mal configuradas (ex.: 00:00) deixam de gerar centenas de horas de atraso.

### O que você precisa fazer
1. **Recalcular o fechamento**  
   O resumo que você vê (246h 40m) foi salvo **antes** dessa regra ou com escala errada. Para atualizar:
   - Na tela de Fechamento, com empresa e funcionário selecionados (janeiro/2026), clique de novo em **“Fechar Mês”**.  
   - O sistema recalcula e grava de novo; o resumo deve passar a mostrar atraso correto (ou 0h se a escala estiver 00:00 e a proteção entrar em ação).

2. **Conferir a escala da empresa**  
   Verificar na base ou na tela de configuração qual escala está vinculada à empresa “chris” (e ao funcionário “aaa”) e ver o **horário de entrada**:
   - Se estiver **00:00**, corrigir para o horário real (ex.: **08:00** ou **13:00**, conforme o turno).
   - Migrations padrão já criam escala com entrada **08:00**; se mesmo assim aparecer 00:00, pode ter sido alterado manualmente ou por dado legado.

---

## 2. Porcentagem no resumo

### Situação atual
O resumo exibe apenas valores absolutos:

- Horas trabalhadas (ex.: 32,23 / 32h 14m)
- Horas extras (0,00)
- Atrasos (minutos/horas)
- Ausências (ex.: 8h 00m)

Não há **porcentagem** em relação a uma meta (carga horária do mês, dias úteis, etc.).

### Melhoria sugerida
- Incluir no **Resumo do Fechamento** algo como:
  - **Carga do mês:** X% (horas trabalhadas ÷ horas esperadas no mês).
- Horas esperadas no mês = `carga diária da escala × dias com batida` (ou dias úteis do mês, conforme regra de negócio).
- Exemplo: 32h trabalhadas em um mês com 176h esperadas → **~18%** (ou o inverso, “100%” quando bater a meta).

Isso exige:
- Backend: retornar `expectedMinutesInMonth` (ou equivalente) no fechamento, ou o front calcular com `dailyHours` da escala e número de dias.
- Front: exibir a porcentagem ao lado das horas trabalhadas ou em uma linha dedicada.

---

## 3. Outros fluxos de melhoria

### 3.1 Botão “Recalcular” no resumo
- **Problema:** Para corrigir um resumo já fechado (ex.: atrasos errados), o usuário precisa clicar de novo em “Fechar Mês”, o que pode parecer “fechar de novo” em vez de “recalcular”.
- **Melhoria:** Incluir um botão **“Recalcular resumo”** (ou “Atualizar resumo”) quando o mês já estiver fechado. O mesmo endpoint de fechamento pode ser chamado; o texto do botão deixa claro que é recálculo.

### 3.2 Tela/API de escala de trabalho
- Hoje a escala é usada no fechamento, mas não há tela para o usuário **ver ou editar** horário de entrada, saída, tolerância, etc.
- **Melhoria:** Tela (ex.: em Empresas ou Configurações) para listar/editar escalas por empresa e vincular ao funcionário, evitando entrada 00:00 e facilitando ajustes.

### 3.3 Validação ao fechar
- Ao clicar em “Fechar Mês”, validar (opcional):
  - Mínimo de dias com batida ou mínimo de horas na grade.
  - Aviso se houver muitos dias vazios (ex.: “X dias sem nenhuma batida. Deseja mesmo fechar?”).

### 3.4 Exportar resumo (PDF/Excel)
- Permitir **exportar o resumo do fechamento** (e, se fizer sentido, a grade) em PDF ou planilha, para arquivo e envio ao DP/RH/contabilidade.

### 3.5 Histórico de fechamentos
- Listar fechamentos já realizados (por empresa/funcionário ou por mês/ano) e permitir reabrir período apenas para consulta ou, se a regra permitir, para recalcular.

### 3.6 Atrasos por dia (detalhe)
- No resumo, além do total de atrasos, mostrar **quais dias** tiveram atraso e quantos minutos em cada um (lista ou tooltip), para o DP conferir rapidamente.

### 3.7 Ausências
- Hoje “Ausências” é calculado como `dailyHours` (ex.: 8h) por dia sem entrada/saída. Pode ser desejável:
  - Diferenciar **falta** de **folga/feriado** (quando houver cadastro de dias não úteis).
  - Mostrar **quais dias** foram contados como ausência.

---

## 4. Resumo rápido

| Item | Status | Ação |
|------|--------|------|
| 246h atraso | Backend já com proteção (entrada &lt; 05:00 → 0) | Re-fechar o mês (Fechar Mês de novo) e conferir escala (entrada 08:00 ou real) |
| Porcentagem no resumo | Não implementado | Calcular horas esperadas no mês e exibir % no front (e, se necessário, no backend) |
| Botão Recalcular | Sugerido | Adicionar botão “Recalcular resumo” quando mês já fechado |
| Tela de escala | Sugerido | CRUD de escala (entrada, saída, tolerância) por empresa |
| Outros (exportar, histórico, atrasos por dia, ausências por dia) | Sugerido | Priorizar conforme necessidade do DP/RH |
