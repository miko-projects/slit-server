-- ── Username + tag on users ───────────────────────────────────────────────────
-- username: user-chosen handle, not unique on its own  (e.g. "mikos")
-- tag:      4 uppercase hex chars, server-assigned      (e.g. "A3F2")
-- handle = username#tag is globally unique
ALTER TABLE users
    ADD COLUMN username VARCHAR(32),
    ADD COLUMN tag      CHAR(4);

-- Backfill existing rows: derive username from display_name, tag from UUID hash
UPDATE users
SET username = LOWER(REGEXP_REPLACE(display_name, '[^a-zA-Z0-9_]', '', 'g')),
    tag      = UPPER(SUBSTRING(MD5(id::text), 1, 4));

-- Now enforce NOT NULL and uniqueness
ALTER TABLE users
    ALTER COLUMN username SET NOT NULL,
    ALTER COLUMN tag      SET NOT NULL;

ALTER TABLE users ADD CONSTRAINT uq_username_tag UNIQUE (username, tag);

-- ── Friendships ───────────────────────────────────────────────────────────────
CREATE TABLE friendships (
    id           UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    requester_id UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    addressee_id UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    status       VARCHAR(20) NOT NULL DEFAULT 'pending'
                             CHECK (status IN ('pending', 'accepted', 'blocked')),
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT no_self_friend CHECK (requester_id != addressee_id),
    UNIQUE (requester_id, addressee_id)
);

CREATE INDEX idx_friendships_requester ON friendships(requester_id);
CREATE INDEX idx_friendships_addressee ON friendships(addressee_id);
