-- Adicionar campo de status aos eventos de ponto
ALTER TABLE time_events
ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'CONFIRMED';

-- Atualizar eventos existentes para CONFIRMED
UPDATE time_events SET status = 'CONFIRMED' WHERE status IS NULL;

-- Criar índice para consultas por status
CREATE INDEX idx_time_events_status ON time_events(status);

-- Comentário para documentação
COMMENT ON COLUMN time_events.status IS 'Status do evento: CONFIRMED (aprovado), REVIEW (requer revisão), UNCERTAIN (baixa confiança)';
