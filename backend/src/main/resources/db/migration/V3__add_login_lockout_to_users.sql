-- Failed-login tracking for the AUTH-2 lockout: 5 failures within 15 minutes lock the account for 15 minutes.
ALTER TABLE users
    ADD COLUMN failed_login_count        INTEGER     NOT NULL DEFAULT 0,
    ADD COLUMN failed_login_window_start TIMESTAMPTZ,
    ADD COLUMN locked_until              TIMESTAMPTZ;
