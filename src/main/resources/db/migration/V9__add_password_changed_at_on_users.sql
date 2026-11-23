-- Tokens issued before this time are rejected, so changing the password logs out other sessions
ALTER TABLE users
    ADD COLUMN password_changed_at TIMESTAMP NULL;
