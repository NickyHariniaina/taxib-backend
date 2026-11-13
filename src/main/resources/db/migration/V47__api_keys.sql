-- API keys are stored hashed: the plaintext only ever exists in the client's configuration.
-- Rollout is fail-open: while the table holds no active key, the API stays public.

CREATE TABLE IF NOT EXISTS api_keys (
  id BIGSERIAL PRIMARY KEY,
  name VARCHAR(255) NOT NULL UNIQUE,
  key_hash VARCHAR(64) NOT NULL UNIQUE,
  requests_per_minute INT NOT NULL DEFAULT 600,
  requests_per_hour INT NOT NULL DEFAULT 20000,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  revoked_at TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS api_keys_active_idx ON api_keys (key_hash) WHERE revoked_at IS NULL;
