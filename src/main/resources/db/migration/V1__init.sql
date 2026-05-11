-- ── Users ────────────────────────────────────────────────────────────────────
CREATE TABLE users (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email         VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    display_name  VARCHAR(100) NOT NULL,
    scan_credits  INTEGER      NOT NULL DEFAULT 5,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- ── Groups ───────────────────────────────────────────────────────────────────
CREATE TABLE slit_groups (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name          VARCHAR(100) NOT NULL,
    kind          VARCHAR(20)  NOT NULL CHECK (kind IN ('household','trip','event')),
    destination   VARCHAR(255),
    created_by_id UUID         NOT NULL REFERENCES users(id) ON DELETE SET NULL,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE group_members (
    group_id  UUID        NOT NULL REFERENCES slit_groups(id) ON DELETE CASCADE,
    user_id   UUID        NOT NULL REFERENCES users(id)       ON DELETE CASCADE,
    joined_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (group_id, user_id)
);

-- ── Receipts ─────────────────────────────────────────────────────────────────
CREATE TABLE receipts (
    id            UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id       UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    group_id      UUID         REFERENCES slit_groups(id)   ON DELETE SET NULL,
    store_name    VARCHAR(255) NOT NULL,
    store_location VARCHAR(255),
    purchased_at  TIMESTAMPTZ  NOT NULL,
    currency      VARCHAR(3)   NOT NULL DEFAULT 'USD',
    subtotal      NUMERIC(10,2) NOT NULL DEFAULT 0,
    tax           NUMERIC(10,2) NOT NULL DEFAULT 0,
    total         NUMERIC(10,2) NOT NULL DEFAULT 0,
    save_target   VARCHAR(20)  NOT NULL DEFAULT 'personal'
                               CHECK (save_target IN ('personal','trip','household')),
    scan_quality  VARCHAR(20)  NOT NULL DEFAULT 'clear'
                               CHECK (scan_quality IN ('clear','partial','blurry','crumpled')),
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE receipt_items (
    id          UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    receipt_id  UUID          NOT NULL REFERENCES receipts(id) ON DELETE CASCADE,
    name        VARCHAR(255)  NOT NULL,
    qty         NUMERIC(10,4) NOT NULL DEFAULT 1,
    unit_price  NUMERIC(10,2) NOT NULL,
    line_total  NUMERIC(10,2) NOT NULL,
    category    VARCHAR(30)   NOT NULL DEFAULT 'pantry',
    confidence  NUMERIC(4,3)  NOT NULL DEFAULT 1.0,
    qty_label   VARCHAR(50),
    sort_order  INTEGER       NOT NULL DEFAULT 0
);

-- ── Group Expenses ────────────────────────────────────────────────────────────
CREATE TABLE expenses (
    id          UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    group_id    UUID          NOT NULL REFERENCES slit_groups(id) ON DELETE CASCADE,
    receipt_id  UUID          REFERENCES receipts(id) ON DELETE SET NULL,
    title       VARCHAR(255)  NOT NULL,
    amount      NUMERIC(10,2) NOT NULL,
    payer_id    UUID          NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    split_type  VARCHAR(20)   NOT NULL DEFAULT 'equal'
                              CHECK (split_type IN ('equal','percent','amount','item')),
    created_at  TIMESTAMPTZ   NOT NULL DEFAULT now()
);

CREATE TABLE expense_splits (
    id          UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    expense_id  UUID          NOT NULL REFERENCES expenses(id) ON DELETE CASCADE,
    user_id     UUID          NOT NULL REFERENCES users(id)    ON DELETE CASCADE,
    amount_owed NUMERIC(10,2) NOT NULL
);

-- ── Indexes ───────────────────────────────────────────────────────────────────
CREATE INDEX idx_receipts_user_id   ON receipts(user_id);
CREATE INDEX idx_receipts_group_id  ON receipts(group_id);
CREATE INDEX idx_receipt_items_receipt_id ON receipt_items(receipt_id);
CREATE INDEX idx_expenses_group_id  ON expenses(group_id);
CREATE INDEX idx_expense_splits_expense_id ON expense_splits(expense_id);
CREATE INDEX idx_group_members_user_id ON group_members(user_id);
