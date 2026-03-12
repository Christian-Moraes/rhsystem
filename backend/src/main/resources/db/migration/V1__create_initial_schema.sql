-- Tabela de usuários
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_users_email ON users(email);

-- Tabela de empresas
CREATE TABLE companies (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    cnpj VARCHAR(18) NOT NULL UNIQUE,
    responsible_name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    phone VARCHAR(20),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_companies_cnpj ON companies(cnpj);

-- Tabela de relacionamento usuário-empresa
CREATE TABLE user_companies (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    company_id UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    role VARCHAR(20) NOT NULL DEFAULT 'VIEWER',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id, company_id)
);

CREATE INDEX idx_user_companies_user_id ON user_companies(user_id);
CREATE INDEX idx_user_companies_company_id ON user_companies(company_id);

-- Tabela de escalas de trabalho
CREATE TABLE work_schedules (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    daily_hours INTEGER NOT NULL DEFAULT 480, -- em minutos (8 horas)
    entry_time TIME NOT NULL DEFAULT '08:00:00',
    exit_time TIME NOT NULL DEFAULT '17:00:00',
    break_minutes INTEGER NOT NULL DEFAULT 60,
    tolerance_late INTEGER NOT NULL DEFAULT 10, -- minutos
    tolerance_leave INTEGER NOT NULL DEFAULT 10 -- minutos
);

CREATE INDEX idx_work_schedules_company_id ON work_schedules(company_id);

-- Tabela de funcionários
CREATE TABLE employees (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    cpf VARCHAR(14) NOT NULL,
    role VARCHAR(100),
    hire_date DATE NOT NULL,
    work_schedule_id UUID REFERENCES work_schedules(id),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    UNIQUE(company_id, cpf)
);

CREATE INDEX idx_employees_company_id ON employees(company_id);
CREATE INDEX idx_employees_cpf ON employees(cpf);

-- Tabela de eventos de ponto (event-based)
CREATE TABLE time_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    employee_id UUID NOT NULL REFERENCES employees(id) ON DELETE CASCADE,
    event_type VARCHAR(20) NOT NULL,
    event_time TIMESTAMP NOT NULL,
    source VARCHAR(50) NOT NULL DEFAULT 'MANUAL',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Índice composto crítico para performance
CREATE INDEX idx_time_events_employee_time ON time_events(employee_id, event_time);
CREATE INDEX idx_time_events_event_time ON time_events(event_time);

-- Tabela de fechamento mensal (cache de cálculo)
CREATE TABLE timesheets (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    employee_id UUID NOT NULL REFERENCES employees(id) ON DELETE CASCADE,
    month INTEGER NOT NULL CHECK (month >= 1 AND month <= 12),
    year INTEGER NOT NULL,
    worked_hours INTEGER NOT NULL DEFAULT 0, -- em minutos
    extra_hours INTEGER NOT NULL DEFAULT 0, -- em minutos
    delay_minutes INTEGER NOT NULL DEFAULT 0,
    absence_minutes INTEGER NOT NULL DEFAULT 0,
    closed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(employee_id, month, year)
);

CREATE INDEX idx_timesheets_employee ON timesheets(employee_id);
CREATE INDEX idx_timesheets_month_year ON timesheets(year, month);
