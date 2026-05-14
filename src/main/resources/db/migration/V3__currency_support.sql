-- ── Group base currency ───────────────────────────────────────────────────────
ALTER TABLE slit_groups
    ADD COLUMN currency VARCHAR(3) NOT NULL DEFAULT 'USD';

-- ── Expense currency ──────────────────────────────────────────────────────────
ALTER TABLE expenses
    ADD COLUMN currency VARCHAR(3) NOT NULL DEFAULT 'USD';

-- ── Exchange rates (USD-based, refreshed daily) ───────────────────────────────
CREATE TABLE exchange_rates (
    id              UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    base_currency   VARCHAR(3)    NOT NULL,
    target_currency VARCHAR(3)    NOT NULL,
    rate            NUMERIC(18,6) NOT NULL,
    fetched_at      TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT uq_rate_pair UNIQUE (base_currency, target_currency)
);

CREATE INDEX idx_exchange_rates_base ON exchange_rates(base_currency);
