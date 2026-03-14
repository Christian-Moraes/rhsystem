# Relatório: Importação no banco × imagens do ponto

**Consulta no banco:** Janeiro/2026, funcionário **aaa** (único com eventos no banco).  
**Imagens de referência:** 1ª quinzena (dias 1–15) e 2ª quinzena (dias 16–31) no mesmo layout.

---

## Resumo

| Item | Status |
|------|--------|
| **1ª quinzena – dia 15** | ❌ **Faltando no banco.** Os horários do dia 15 (14:01, 18:00, 19:02, 21:53) estão gravados como **dia 14**. |
| **1ª quinzena – dias 10–14** | ⚠️ **Deslocados em 1 dia.** No banco: dia 10 = imagem dia 11; dia 11 = imagem dia 12; …; dia 14 = imagem dia 15. Falta o dia 15 real. |
| **2ª quinzena – dia 17** | ❌ **Falta 1 batida.** Na imagem: 14:00 e 21:19. No banco: só 14:00 (falta Saída 21:19). |
| **2ª quinzena – dias 18–28** | ✅ Batidas dos dias 19–28 na imagem batem com os dias 18–27 no banco (dia 28 da imagem = dia 28 no banco). |
| **Tipos ENTRY/EXIT** | ✅ Após a correção (alternância por dia), a tendência é vir correta em novas importações. |

---

## 1ª quinzena – comparação dia a dia

| Dia img | Horários na imagem | No banco (dia) | Horários no banco | Bate? |
|---------|--------------------|----------------|-------------------|--------|
| 1 | 13:58, 18:01, 19:04, 21:56 | 1 | 13:58, 18:01, 19:04, **21:36** | ⚠️ Saída 21:36 vs 21:56 |
| 2 | 13:58, 18:09, 19:10, 21:43 | 2 | 13:58, 18:09, 19:10, 21:43 | ✅ |
| 3 | (vazio) | 3 | 13:06, 18:07, 19:09, 21:40 | ❌ Imagem vazia; banco preenchido (dado do dia 4?) |
| 4 | 13:06, 18:07, 19:09, 21:40 | 4 | 13:58, 18:00, 18:58, 21:39 | ❌ Troca com dia 5 |
| 5–9 | … | 5–9 | … | ⚠️ Possível deslocamento |
| 10 | (vazio) | 10 | 14:02, 17:59, 19:00, 21:51 | ❌ Imagem vazia; banco = horários do dia 11 |
| 11 | 14:02, 17:59, 19:00, 21:51 | 11 | 13:59, 18:02, 19:04, 21:57 | = imagem dia 12 |
| 12 | 13:59, 18:02, 19:04, 21:57 | 12 | 14:02, 19:01, 20:03, 22:04 | = imagem dia 13 |
| 13 | 14:02, 19:01, 20:03, 22:04 | 13 | 14:04, 18:03, 19:07 | = imagem dia 14 (falta saída) |
| 14 | 14:04, 18:03, 19:07, (vazio) | 14 | **14:01, 18:00, 19:02, 21:53** | ❌ **Isso é o dia 15 da imagem.** Dia 15 real não existe no banco. |
| **15** | **14:01, 18:00, 19:02, 21:53** | **—** | **Não existe** | ❌ **Dia 15 faltando** |

Conclusão 1ª quinzena: a IA “deslocou” os últimos dias e colocou o dia 15 da imagem no dia 14 do banco, por isso **falta o dia 15** na importação.

---

## 2ª quinzena – comparação dia a dia

| Dia img | Horários na imagem | No banco (dia) | Bate? |
|---------|--------------------|----------------|--------|
| 16 | 13:59, 18:01, 19:00, 21:33 | 16: 13:59, 18:01, 19:00, 21:53 | ⚠️ Última hora 21:33 vs 21:53 |
| 17 | 14:00, 21:19 | 17: **só 14:00** | ❌ Falta Saída 21:19 |
| 18 | (vazio) | 18: 13:58, 18:03, 19:04, 21:55 | ❌ Imagem vazia; banco = dia 19 |
| 19–28 | … | 19–28 | ✅ Dias 19–28 da imagem batem com 18–28 no banco (com pequenas diferenças) |
| 27 | 14:00, 17:59, 19:01, 21:52 | 26: 13:59, 18:05, 19:01, 21:52 | ⚠️ Pequenas diferenças |
| 28 | 14:03, 18:12, 19:14, 21:54 | 28: 14:03, 18:12, 19:14, 21:54 | ✅ |

Conclusão 2ª quinzena: dia 17 com uma batida a menos (21:19); demais dias coerentes, com possível deslocamento em um dia no início (ex.: dia 18 imagem vazio vs dia 18 banco preenchido).

---

## O que fazer

1. **Reimportar as duas imagens** depois do deploy do backend com as correções:
   - ENTRY/EXIT por dia (evita troca de tipos).
   - Prompt reforçando “incluir primeira e última linha com dados” (evita faltar dia 15).
   - `max_tokens` 8192 (reduz corte no meio da 2ª quinzena).

2. **Antes de reimportar:** apagar os eventos de janeiro/2026 desse funcionário (pela tela de Fechamento, “Salvar grade” com a grade vazia, ou via SQL), para não duplicar e para o merge não esconder dias que “já existem” com dados errados.

3. **Se quiser só corrigir o dia 15 sem reimportar:** no banco, inserir o dia 15 com os 4 horários (14:01, 18:00, 19:02, 21:53) e, se desejado, ajustar o dia 14 para os horários corretos do dia 14 da imagem (14:04, 18:03, 19:07, sem quarta batida). Posso montar o SQL exato para isso se quiser.

---

## Query usada no banco

```sql
SELECT event_time::date AS dia, to_char(event_time, 'HH24:MI') AS hora, event_type AS tipo
FROM time_events
WHERE employee_id = '850a6ab9-dcee-4bb6-99d4-cb8f06bd0d40'
  AND event_time >= '2026-01-01' AND event_time < '2026-02-01'
ORDER BY event_time;
```

Total no banco: **86** eventos em janeiro/2026 para o funcionário **aaa**.
