ALTER TABLE users ADD COLUMN username VARCHAR(32);

-- Give every existing account a collision-free fallback without changing email or credentials.
UPDATE users SET username = 'user_' || id;

-- Preserve a suitable unique display name when it can safely be used as a login name.
UPDATE users AS candidate
SET username = lower(candidate.display_name)
WHERE lower(candidate.display_name) ~ '^[a-z0-9][a-z0-9_.-]{2,31}$'
  AND NOT EXISTS (
      SELECT 1 FROM users AS other
      WHERE other.id <> candidate.id
        AND (lower(other.display_name) = lower(candidate.display_name)
             OR other.username = lower(candidate.display_name))
  );

ALTER TABLE users ALTER COLUMN username SET NOT NULL;
ALTER TABLE users ADD CONSTRAINT users_username_format
    CHECK (username ~ '^[a-z0-9][a-z0-9_.-]{2,31}$');
CREATE UNIQUE INDEX users_username_lower_idx ON users (lower(username));
