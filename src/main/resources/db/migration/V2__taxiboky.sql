-- TaxiBoky ATT 2013 reference tables: lines, ordered directions and stops,
-- plus the "MISY MIALA Ô" quartier index (filled later).
-- Idempotent: safe to apply where the tables were created manually.

CREATE TABLE IF NOT EXISTS book_lines (
  id BIGSERIAL PRIMARY KEY,
  ref VARCHAR(64) NOT NULL UNIQUE,
  operateur VARCHAR(255),
  tel VARCHAR(64),
  source VARCHAR(64) NOT NULL DEFAULT 'ATT-2013'
);

CREATE TABLE IF NOT EXISTS book_directions (
  id BIGSERIAL PRIMARY KEY,
  line_id BIGINT NOT NULL REFERENCES book_lines (id) ON DELETE CASCADE,
  nom VARCHAR(255) NOT NULL,
  sens VARCHAR(16) NOT NULL,
  UNIQUE (line_id, sens)
);

CREATE TABLE IF NOT EXISTS book_stops (
  id BIGSERIAL PRIMARY KEY,
  direction_id BIGINT NOT NULL REFERENCES book_directions (id) ON DELETE CASCADE,
  seq INT NOT NULL,
  nom VARCHAR(255) NOT NULL,
  UNIQUE (direction_id, seq)
);
CREATE INDEX IF NOT EXISTS book_stops_direction_idx ON book_stops (direction_id);

CREATE TABLE IF NOT EXISTS book_quartiers (
  id BIGSERIAL PRIMARY KEY,
  nom VARCHAR(255) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS book_quartier_lignes (
  quartier_id BIGINT NOT NULL REFERENCES book_quartiers (id) ON DELETE CASCADE,
  line_id BIGINT NOT NULL REFERENCES book_lines (id) ON DELETE CASCADE,
  PRIMARY KEY (quartier_id, line_id)
);
