-- TaxiB schema v1: PostGIS + GTFS-lite transport tables.
--
-- PostGIS installs into the default schema (public on Neon).
-- Enable once per database if the dashboard didn't: CREATE EXTENSION IF NOT EXISTS postgis;

CREATE EXTENSION IF NOT EXISTS postgis;

CREATE TABLE stops (
  id BIGSERIAL PRIMARY KEY,
  osm_id VARCHAR(64) NOT NULL UNIQUE,
  name VARCHAR(255) NOT NULL,
  geom geography(Point, 4326) NOT NULL
);
CREATE INDEX stops_geom_gix ON stops USING GIST (geom);

CREATE TABLE lines (
  id BIGSERIAL PRIMARY KEY,
  ref VARCHAR(64) NOT NULL UNIQUE,
  name VARCHAR(255)
);

CREATE TABLE route_variants (
  id BIGSERIAL PRIMARY KEY,
  line_id BIGINT NOT NULL REFERENCES lines (id),
  osm_relation_id BIGINT NOT NULL UNIQUE,
  geom geography(LineString, 4326)
);

CREATE TABLE variant_stops (
  id BIGSERIAL PRIMARY KEY,
  variant_id BIGINT NOT NULL REFERENCES route_variants (id),
  stop_id BIGINT NOT NULL REFERENCES stops (id),
  seq INT NOT NULL,
  UNIQUE (variant_id, seq)
);
CREATE INDEX variant_stops_stop_idx ON variant_stops (stop_id);
