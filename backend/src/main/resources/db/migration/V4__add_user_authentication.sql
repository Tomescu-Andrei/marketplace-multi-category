ALTER TABLE users ADD COLUMN password_hash VARCHAR(100);
CREATE UNIQUE INDEX users_email_lower_idx ON users (lower(email));

-- Existing M1 sellers intentionally have no password and cannot log in.
-- Newly registered accounts always receive a hash through the authentication service.
