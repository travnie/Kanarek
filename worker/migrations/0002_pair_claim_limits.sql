-- Persistent per-client limit for one-time pairing code claims.
-- The Worker creates this table lazily when migrations are not applied.
CREATE TABLE IF NOT EXISTS pair_claim_limits (
  client_key TEXT PRIMARY KEY,
  window_bucket INTEGER NOT NULL,
  attempts INTEGER NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_pair_claim_limits_window ON pair_claim_limits(window_bucket);
