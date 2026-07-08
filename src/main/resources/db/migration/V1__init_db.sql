-- Создание таблицы обязательств
CREATE TABLE obligations (
    id UUID PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    amount NUMERIC(19, 2) NOT NULL,
    currency VARCHAR(10) NOT NULL,
    category VARCHAR(50) NOT NULL,
    recurrence VARCHAR(50),
    next_payment_date DATE NOT NULL,
    status VARCHAR(50) NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL,
    original_subscription_start_day INTEGER
);

-- Создание таблицы платежей
CREATE TABLE payments (
    id UUID PRIMARY KEY,
    obligation_id UUID NOT NULL,
    amount NUMERIC(19, 2) NOT NULL,
    currency VARCHAR(10) NOT NULL,
    paid_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_payments_obligation FOREIGN KEY (obligation_id) REFERENCES obligations (id) ON DELETE CASCADE
);

-- Индекс для быстрого поиска дубликатов без учета регистра (требование ТЗ)
CREATE INDEX idx_obligations_title_lower ON obligations (LOWER(title));

-- Индекс для внешнего ключа (лучшая практика для ускорения JOIN-ов)
CREATE INDEX idx_payments_obligation_id ON payments (obligation_id);