-- daily_hours é MINUTOS (V1). Registros criados pela API com "8" (horas) ficaram como 8 minutos e quebram o fechamento.
-- Multiplica apenas valores que claramente eram "horas" digitadas (1 a 24).
UPDATE work_schedules
SET daily_hours = daily_hours * 60
WHERE daily_hours IS NOT NULL
  AND daily_hours BETWEEN 1 AND 24;
