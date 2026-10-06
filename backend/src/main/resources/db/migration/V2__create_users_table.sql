CREATE TABLE users (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name          VARCHAR(100)  NOT NULL,
    email         VARCHAR(254)  NOT NULL,
    password_hash VARCHAR(100)  NOT NULL,
    avatar_url    VARCHAR(2048),
    system_role   VARCHAR(32)   NOT NULL DEFAULT 'USER',
    active        BOOLEAN       NOT NULL DEFAULT true,
    created_at    TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT ck_users_system_role CHECK (system_role IN ('ADMIN', 'USER'))
);

-- Emails are unique regardless of case.
CREATE UNIQUE INDEX uq_users_email_lower ON users (lower(email));
