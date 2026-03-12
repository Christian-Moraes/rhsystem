-- Insere uma escala padrão para cada empresa existente
INSERT INTO work_schedules (company_id, daily_hours, entry_time, exit_time, break_minutes, tolerance_late, tolerance_leave)
SELECT 
    id,
    480, -- 8 horas
    '08:00:00',
    '17:00:00',
    60, -- 1 hora de intervalo
    10, -- 10 minutos de tolerância
    10
FROM companies
WHERE id NOT IN (SELECT DISTINCT company_id FROM work_schedules);
