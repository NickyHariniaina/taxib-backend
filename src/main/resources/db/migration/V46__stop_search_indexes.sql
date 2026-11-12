-- Indexes for the two hot stop filters:
--   * the neighborhood lookup (GET /api/neighborhoods/{name}/stops)
--   * the bounding-box search (GET /api/stops?lat&lon&radius)
-- Both are plain scans on 868 rows today, and become index scans as the data grows.

CREATE INDEX IF NOT EXISTS stops_neighborhood_idx ON stops (neighborhood);

CREATE INDEX IF NOT EXISTS stops_lat_lng_idx ON stops (latitude, longitude);
