# Conferência: dias e horários esperados das folhas de ponto

Referência para conferir se a importação bate com as imagens.

---

## Imagem 1 — 1ª Quinzena (Fevereiro 2026) — Natalia

| Dia | Horários esperados (ordem na folha) | Observação |
|-----|-------------------------------------|------------|
| 1   | 13:58, 18:01, 19:04, 21:56          | 4 batidas  |
| 2   | 13:58, 18:09, 19:10, 21:43          | 4 batidas  |
| 3   | —                                   | linha em branco |
| 4   | 13:06, 18:07, 19:09, 21:40          | 4 batidas  |
| 5   | 13:58, 18:00, 18:58, 21:39          | 4 batidas  |
| 6   | 13:58, 18:01, 19:04, 21:55          | 4 batidas  |
| 7   | 13:58, 18:00, 19:00, 21:55          | 4 batidas  |
| 8   | —                                   | linha em branco |
| 9   | 14:14                               | só 1 batida (Hora Normal) |
| 10  | —                                   | linha em branco |
| 11  | 14:02, 17:59, 19:00, 21:51          | 4 batidas  |
| 12  | 13:59, 18:02, 19:04, 21:57          | 4 batidas  |
| 13  | 14:02, 19:01, 20:03, 22:04          | 4 batidas  |
| 14  | 14:04, 18:03, 19:07                 | 3 batidas (falta Saída Tarde) |
| 15  | 14:01, 18:00, 19:02, 21:53          | 4 batidas — conferir se não caiu no dia 14 |

**Total 1ª quinzena:** dias 1, 2, 4, 5, 6, 7, 9, 11, 12, 13, 14, 15 com horários (dia 15 obrigatório na lista).

---

## Imagem 2 — 2ª Quinzena (mesmo mês) — mesma folha

| Dia | Horários esperados (ordem na folha) | Observação |
|-----|-------------------------------------|------------|
| 16  | 13:59, 18:01, 19:00, 21:53          | 4 batidas  |
| 17  | 14:00, 21:19                        | só 2 batidas (Manhã Saída e Tarde Entrada vazios) — **conferir se 21:19 aparece** |
| 18  | —                                   | linha em branco |
| 19  | 13:58, 18:03, 19:04, 21:55          | 4 batidas  |
| 20  | 13:59, 18:00, 19:03, 21:42          | 4 batidas  |
| 21  | 14:02, 17:59, 19:00, 21:58          | 4 batidas  |
| 22  | 14:04, 18:02, 19:05, 21:45          | 4 batidas  |
| 23  | 14:00                               | só 1 batida  |
| 24  | —                                   | linha em branco |
| 25  | 13:59, 18:08, 19:09                 | 3 batidas (Tarde Saída vazio) |
| 26  | 13:59, 18:05, 19:08                 | 3 batidas (Tarde Saída vazio) |
| 27  | 14:00, 17:59, 19:01, 21:52          | 4 batidas  |
| 28  | 14:03, 18:12, 19:14, 21:54          | 4 batidas  |
| 29  | —                                   | em branco  |
| 30  | —                                   | em branco  |
| 31  | —                                   | em branco  |

**Total 2ª quinzena:** dias 16, 17, 19, 20, 21, 22, 23, 25, 26, 27, 28 com horários. Dia 17 deve ter **duas** batidas: 14:00 e **21:19**.

---

## O que conferir após importar

1. **Fev/2026, Natalia (1ª quinzena)**  
   - Dia **15** deve existir com exatamente: 14:01, 18:00, 19:02, 21:53 (e não no dia 14).  
   - Dia 14 deve ter só 3 horários: 14:04, 18:03, 19:07.  
   - No log da importação: `dias no JSON (days[]):` deve incluir **15**.

2. **Mesmo mês (2ª quinzena)**  
   - Dia **17** deve ter **duas** batidas: 14:00 e **21:19**.  
   - Se faltar 21:19, a IA ou o parser pode estar ignorando a segunda batida quando há células vazias no meio.

3. **Tipos ENTRY/EXIT**  
   - Por dia: 1ª batida = ENTRY, 2ª = EXIT, 3ª = ENTRY, 4ª = EXIT (e assim por diante, reiniciando a cada dia).

Use este arquivo para comparar com a listagem de eventos no banco ou na tela de ponto após a importação.
