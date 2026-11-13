-- Bus lines: the position of each stop along a line variant.
--
-- Until now bus_line_stops was a plain (line_id, stop_id) set, so the API could
-- only list a line's stops alphabetically, mixing both directions. The order is
-- what lets a client slice the ride between two stops.
--
-- Nullable on purpose: a link imported before the ordered sweep keeps NULL
-- instead of getting a made-up position. The import (fetch-taxib-stops.py)
-- rebuilds every link with a contiguous 0..n-1 order per variant.
--
-- Idempotent: it can be applied by hand before the application ever boots.

ALTER TABLE bus_line_stops
    ADD COLUMN IF NOT EXISTS stop_order INT;

-- Postgres allows repeated NULLs, so legacy rows do not block the constraint.
CREATE UNIQUE INDEX IF NOT EXISTS bus_line_stops_order_uc
    ON bus_line_stops (line_id, stop_order);
