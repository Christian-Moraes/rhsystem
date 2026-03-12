-- Adicionar configurações de ponto na tabela companies
ALTER TABLE companies
ADD COLUMN enable_overtime BOOLEAN NOT NULL DEFAULT true,
ADD COLUMN enable_time_bank BOOLEAN NOT NULL DEFAULT false,
ADD COLUMN overtime_percentage INTEGER NOT NULL DEFAULT 50, -- 50% sobre hora normal
ADD COLUMN weekend_percentage INTEGER NOT NULL DEFAULT 50; -- 50% sobre hora normal

-- Comentários para documentação
COMMENT ON COLUMN companies.enable_overtime IS 'Habilita cálculo de horas extras';
COMMENT ON COLUMN companies.enable_time_bank IS 'Habilita banco de horas';
COMMENT ON COLUMN companies.overtime_percentage IS 'Porcentagem da hora extra sobre hora normal (ex: 50 = 50%)';
COMMENT ON COLUMN companies.weekend_percentage IS 'Porcentagem adicional para fim de semana (ex: 50 = 50%)';
