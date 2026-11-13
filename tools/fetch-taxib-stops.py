#!/usr/bin/env python3
"""Refresh the TaxiB reference data snapshots (neighborhoods, stops, bus lines).

Snapshots land in the directory given by --outdir and are the reproducible source
of the data loaded into PostgreSQL:

    taxib-neighborhoods-<date>.json   districts (quartiers) with their stop count
    taxib-stops-<date>.json           geolocated stops with the lines serving them
    taxib-lines-<date>.json           bus lines and their directions
    taxib-routes-<date>.json          ordered stops of every line variant

Usage
-----
    export TAXIB_DATA_BASE_URL='https://<provider>/api/v2/taxi-be'
    export TAXIB_DATA_API_KEY='<key>'
    ./fetch-taxib-stops.py                          # fetch from the provider + write snapshots
    ./fetch-taxib-stops.py --from-json <stops.json> \
        --routes-json <routes.json>                 # re-emit snapshots, no network calls
    ./fetch-taxib-stops.py --emit-sql <out.sql>     # also write the stops INSERT script
    ./fetch-taxib-stops.py --emit-lines-sql <out.sql>  # bus lines + ordered links

Every column/key of the generated files is English; provider field names are
translated at the boundary.

Line geometry (ordered stops, road polyline) is fetched per variant, from the
route endpoint named by TAXIB_DATA_ROUTE_PATH (default: ``/lignes/{id}``). It is
the only source of stop order: the stop snapshots carry line *labels*, never a
sequence. A route payload that holds no stop list is reported, never guessed.
"""

from __future__ import annotations

import argparse
import datetime as dt
import json
import os
import sys
import time
import urllib.error
import urllib.parse
import urllib.request
from collections import Counter, defaultdict

UA = "taxib-sync/1.0"

# Cell geometry for the stop sweep: 0.022 deg lat (~2.45 km) x 0.0213 deg lng (~2.24 km)
# = 5.5 km2, just under the provider's 6 km2 bbox limit.
CELL_LAT = 0.022
CELL_LNG = 0.0213
MAX_STOPS_PER_CALL = 60
MARGIN_DEG = 0.02  # grid padding around the district centres


class DataClient:
    """Minimal HTTP client for the taxi-be data provider."""

    def __init__(self, base_url: str, api_key: str, min_interval: float = 1.05):
        self.base_url = base_url.rstrip("/")
        self.api_key = api_key
        self.min_interval = min_interval
        self.calls = 0
        self.errors: list[str] = []
        self.stopped = False
        self._last = 0.0

    def get(self, path: str, params: dict) -> dict:
        url = self.base_url + path + ("?" + urllib.parse.urlencode(params) if params else "")
        for _ in range(5):
            wait = self.min_interval - (time.monotonic() - self._last)
            if wait > 0:
                time.sleep(wait)
            req = urllib.request.Request(
                url,
                headers={
                    "X-Api-Key": self.api_key,
                    "Accept": "application/json",
                    "User-Agent": UA,
                },
            )
            try:
                with urllib.request.urlopen(req, timeout=30) as resp:
                    self.calls += 1
                    self._last = time.monotonic()
                    return json.loads(resp.read())
            except urllib.error.HTTPError as exc:
                body = exc.read().decode("utf-8", "replace")
                if exc.code == 429:
                    retry = int(exc.headers.get("Retry-After", "5"))
                    if retry > 120:
                        self.stopped = True
                        self.errors.append(f"{path} -> quota exhausted, retry in {retry}s")
                        return {}
                    time.sleep(retry + 1)
                    continue
                self.errors.append(f"{path} {params} -> HTTP {exc.code}: {body[:120]}")
                return {}
            except Exception as exc:  # noqa: BLE001
                self.errors.append(f"{path} {params} -> {exc}")
                time.sleep(2)
        self.errors.append(f"{path} {params} -> retries exhausted")
        return {}

    def neighborhoods(self) -> list[dict]:
        return self.get("/quartiers", {}).get("valiny", [])

    def stops_in_bbox(self, min_lng, min_lat, max_lng, max_lat) -> list[dict]:
        body = self.get("/arrets", {"bbox": f"{min_lng},{min_lat},{max_lng},{max_lat}"})
        return body.get("valiny", [])

    def stops_by_name(self, name: str) -> list[dict]:
        return self.get("/arrets", {"q": name}).get("valiny", [])

    def lines_by_area(self, name: str) -> list[dict]:
        return self.get("/lignes", {"fiantsonana": name}).get("valiny", [])

    def route(self, variant_id) -> list[dict]:
        """Ordered stops of one variant, or [] when the provider has no route."""
        template = os.environ.get("TAXIB_DATA_ROUTE_PATH", "/lignes/{id}")
        path = template.format(id=urllib.parse.quote(str(variant_id)))
        return _route_stops(self.get(path, {}))


# Keys a provider route payload may nest its ordered stop list under, best first.
_ROUTE_STOP_KEYS = ("fijanonana", "fiantsonana", "stops", "parcours", "arrets")


def _route_stops(body: dict) -> list[dict]:
    """The ordered stop list of a route payload, whatever key holds it."""
    payload = body.get("valiny", body)
    if isinstance(payload, list):
        return payload
    if not isinstance(payload, dict):
        return []
    for key in _ROUTE_STOP_KEYS:
        value = payload.get(key)
        if isinstance(value, list) and value:
            return value
    return []



def _stop_record(s: dict) -> dict:
    if "name" in s:
        return {
            "source_id": s.get("source_id"),
            "name": s["name"],
            "neighborhood": s["neighborhood"],
            "latitude": s["latitude"],
            "longitude": s["longitude"],
            "lines": s.get("lines", []),
        }
    return {
        "source_id": s["id"],
        "name": s["anarana"],
        "neighborhood": s["fokontany"],
        "latitude": s["lat"],
        "longitude": s["lng"],
        "lines": s.get("zotra", []),
    }


def _neighborhood_record(n: dict) -> dict:
    if "name" in n:
        return {
            "name": n["name"],
            "stop_count": n["stop_count"],
            "lines": n.get("lines", []),
            "latitude": n["latitude"],
            "longitude": n["longitude"],
        }
    return {
        "name": n["anarana"],
        "stop_count": n["isan_ny_fijanonana"],
        "lines": n.get("zotra", []),
        "latitude": n["lat"],
        "longitude": n["lng"],
    }


def _line_record(v: dict) -> dict:
    if "name" in v:
        record = {k: v[k] for k in ("number", "name", "direction", "origin", "terminus", "color", "stop_count") if k in v}
    else:
        record = {
            "number": v["zotra"].split()[0],
            "name": v["zotra"],
            "direction": v.get("lalana"),
            "origin": v.get("niaingana"),
            "terminus": v.get("fiafarana"),
            "color": v.get("loko"),
            "stop_count": v.get("isan_ny_fijanonana"),
        }
    # The provider id is what addresses a variant on the route endpoint; without
    # it an ordered sweep is impossible, so the snapshot keeps it.
    if v.get("id") is not None:
        record["source_id"] = str(v["id"])
    return record


def _route_stop_record(s: dict) -> dict | None:
    """A route stop reduced to what the link inserts match on.

    Route stops carry no identifier: only a name, a district and coordinates, which is
    exactly what the link insert matches on.
    """
    try:
        if "name" in s:
            return {"name": s["name"], "latitude": s["latitude"], "longitude": s["longitude"]}
        return {"name": s["anarana"], "latitude": s["lat"], "longitude": s["lng"]}
    except (KeyError, TypeError):
        return None


def sweep_stops(client: DataClient, neighborhoods: list[dict]) -> dict[str, dict]:
    """Bbox sweep over the whole city, splitting any cell that hits the stop cap."""
    lat0 = min(n["lat"] for n in neighborhoods) - MARGIN_DEG
    lat1 = max(n["lat"] for n in neighborhoods) + MARGIN_DEG
    lng0 = min(n["lng"] for n in neighborhoods) - MARGIN_DEG
    lng1 = max(n["lng"] for n in neighborhoods) + MARGIN_DEG

    stops: dict[str, dict] = {}

    def cover(a_lat, b_lat, a_lng, b_lng, depth: int = 0) -> None:
        rows = client.stops_in_bbox(a_lng, a_lat, b_lng, b_lat)
        if len(rows) >= MAX_STOPS_PER_CALL and depth < 3:
            m_lat, m_lng = (a_lat + b_lat) / 2, (a_lng + b_lng) / 2
            for quad in (
                (a_lat, m_lat, a_lng, m_lng),
                (a_lat, m_lat, m_lng, b_lng),
                (m_lat, b_lat, a_lng, m_lng),
                (m_lat, b_lat, m_lng, b_lng),
            ):
                cover(*quad, depth=depth + 1)
            return
        for row in rows:
            stops[row["id"]] = row

    lat = lat0
    while lat < lat1 - 1e-9:
        lng = lng0
        while lng < lng1 - 1e-9:
            cover(lat, min(lat + CELL_LAT, lat1), lng, min(lng + CELL_LNG, lng1))
            lng += CELL_LNG
        lat += CELL_LAT
    return stops


def fill_gaps(client: DataClient, neighborhoods: list[dict], stops: dict[str, dict]) -> int:
    """Top up districts whose stop count does not match the provider's expectation."""
    per = Counter(s["fokontany"] for s in stops.values())
    added = 0
    for n in neighborhoods:
        if per.get(n["anarana"], 0) >= n["isan_ny_fijanonana"]:
            continue
        for row in client.stops_by_name(n["anarana"]):
            if row["id"] not in stops:
                stops[row["id"]] = row
                added += 1
    return added


def sweep_lines(client: DataClient, neighborhoods: list[dict]) -> dict[str, dict]:
    """Every bus line variant, collected per district and de-duplicated by its id."""
    variants: dict[str, dict] = {}
    for n in neighborhoods:
        for v in client.lines_by_area(n["anarana"]):
            variants[v["id"]] = v
    return variants


def sweep_routes(client: DataClient, variants: dict[str, dict]) -> list[dict]:
    """Ordered stops of every variant, one route call each.

    A variant whose route comes back empty is skipped and reported: a variant
    without a route keeps its unordered links rather than getting a made-up
    sequence.
    """
    routes: list[dict] = []
    missing: list[str] = []

    for variant in variants.values():
        if client.stopped:
            break
        stops = [_route_stop_record(s) for s in client.route(variant["id"])]
        stops = [s for s in stops if s]
        if not stops:
            missing.append(
                "{} {} ({})".format(
                    variant.get("zotra", "?"), variant.get("lalana", "?"), variant["id"]
                )
            )
            continue
        record = _line_record(variant)
        record["stops"] = stops
        routes.append(record)

    if missing:
        print(
            "variants without a route ({}):".format(len(missing)),
            *missing[:10],
            sep="\n  ",
            file=sys.stderr,
        )
    return routes


def sql_literal(value: str) -> str:
    return value.replace("'", "''")


def build_sql(stops: list[dict], expected: dict[str, int]) -> str:
    rows = [
        "('{}', {}, {}, '{}')".format(
            sql_literal(s["name"]),
            s["latitude"],
            s["longitude"],
            sql_literal(s["neighborhood"]),
        )
        for s in stops
    ]
    out = [
        "-- ============================================================================",
        "-- TaxiB - bus stops import",
        "--",
        "-- generated : by fetch-taxib-stops.py",
        "-- contents  : {} stops across {} districts".format(len(rows), len(expected)),
        "-- target    : stops(name, latitude, longitude, neighborhood)",
        "--",
        "-- Single transaction (BEGIN/COMMIT). Re-runnable: TRUNCATE clears first.",
        "-- Verification queries at the bottom, run them after COMMIT.",
        "-- ============================================================================",
        "",
        "BEGIN;",
        "",
        "TRUNCATE TABLE stops RESTART IDENTITY;",
        "",
    ]
    for i in range(0, len(rows), 100):
        chunk = rows[i : i + 100]
        out.append("-- stops {}-{} ({} rows)".format(i + 1, i + len(chunk), len(chunk)))
        out.append("INSERT INTO stops (name, latitude, longitude, neighborhood) VALUES")
        out.append(",\n".join(chunk) + ";")
        out.append("")
    out.append("COMMIT;")
    out.append("")
    out.append("-- =============================== VERIFICATION ===============================")
    out.append("-- 1. total rows (expect {})".format(len(rows)))
    out.append("SELECT count(*) AS total_stops FROM stops;")
    out.append("")
    out.append("-- 2. no nulls / no bad values (expect 0)")
    out.append("SELECT count(*) AS bad_rows FROM stops")
    out.append(" WHERE name IS NULL OR latitude IS NULL OR longitude IS NULL")
    out.append("    OR name = '' OR length(name) > 255;")
    out.append("")
    out.append("-- 3. every stop maps to a known district (expect 0)")
    out.append("SELECT count(*) AS orphan_stops")
    out.append("  FROM stops s LEFT JOIN neighborhoods n ON n.name = s.neighborhood")
    out.append(" WHERE n.id IS NULL;")
    out.append("")
    out.append("-- 4. per-district actual vs expected count (expect 0 rows)")
    vals = ",\n".join(
        "    ('{}', {})".format(sql_literal(name), count)
        for name, count in sorted(expected.items())
    )
    out.append("SELECT e.name, e.expected, count(s.id) AS actual")
    out.append("  FROM (VALUES\n" + vals + "\n  ) AS e(name, expected)")
    out.append("  LEFT JOIN neighborhoods n ON n.name = e.name")
    out.append("  LEFT JOIN stops s ON s.neighborhood = n.name")
    out.append(" GROUP BY e.name, e.expected")
    out.append("HAVING count(s.id) <> e.expected;")
    out.append("")
    out.append("-- 5. top 10 districts by stop count")
    out.append("SELECT neighborhood, count(*) FROM stops GROUP BY 1 ORDER BY 2 DESC LIMIT 10;")
    out.append("-- ============================================================================")
    return "\n".join(out) + "\n"


def build_lines_sql(lines: list[dict], routes: list[dict], stops: list[dict], date: str) -> str:
    """Bus lines + their stop links, in route order.

    A link is emitted only for a stop that exists in the stops snapshot, matched
    on (name, latitude, longitude) — the same predicate the hand-written import
    used, because the stops table stores no provider id.
    """
    known = {(s["name"], s["latitude"], s["longitude"]) for s in stops}

    rows: list[str] = []
    catalogue: list[str] = []
    unmatched: list[str] = []
    ordered_variants: list[str] = []
    dropped: list[str] = []

    for line in lines:
        catalogue.append(
            "('{}', '{}', '{}', '{}', '{}', '{}', {})".format(
                sql_literal(line["number"]),
                sql_literal(line["name"]),
                sql_literal(line["direction"]),
                sql_literal(line["origin"]),
                sql_literal(line["terminus"]),
                sql_literal(line["color"]),
                line["stop_count"],
            )
        )

    for route in routes:
        ordered_variants.append(
            "('{}', '{}', '{}', '{}')".format(
                sql_literal(route["name"]),
                sql_literal(route["direction"]),
                sql_literal(route["origin"]),
                sql_literal(route["terminus"]),
            )
        )

        seen: set[tuple] = set()
        order = 0
        stops_of_route = route["stops"] if isinstance(route["stops"], list) else []

        for stop in stops_of_route:
            stop_key = (stop["name"], stop["latitude"], stop["longitude"])
            if stop_key in seen:
                # A loop line revisits some stops; the join table cannot hold the
                # same pair twice, so the first visit keeps its position.
                dropped.append("{} · {} (revu)".format(route["name"], stop["name"]))
                continue
            seen.add(stop_key)

            if stop_key not in known:
                unmatched.append("{} · {}".format(route["name"], stop["name"]))
                continue

            rows.append(
                "('{}', '{}', '{}', '{}', {}, '{}', {}, {})".format(
                    sql_literal(route["name"]),
                    sql_literal(route["direction"]),
                    sql_literal(route["origin"]),
                    sql_literal(route["terminus"]),
                    order,
                    sql_literal(stop["name"]),
                    stop["latitude"],
                    stop["longitude"],
                )
            )
            order += 1

    out = [
        "-- ============================================================================",
        "-- TaxiB - bus lines import (ordered)",
        "--",
        "-- generated : by tools/fetch-taxib-stops.py",
        "-- date      : {}".format(date),
        "-- contents  : {} line variants / {} ordered links".format(len(lines), len(rows)),
        "-- target    : bus_lines, bus_line_stops(line_id, stop_id, stop_order)",
        "--",
        "-- Ordered links only: a variant with no route keeps its existing links and",
        "-- is listed in verification 6. Unmatched stops and revisits are listed below",
        "-- as comments; a variant losing more than a tenth of its stops is a data bug.",
        "--",
        "-- Single transaction (BEGIN/COMMIT). Re-runnable: the variants in this file",
        "-- are cleared before their links are re-inserted.",
        "-- Verification queries at the bottom, run them after COMMIT.",
        "-- ============================================================================",
        "",
    ]

    out.append("-- variants without a route here: see verification 6")
    out.append("-- unmatched stops ({}):".format(len(unmatched)))
    out.extend("--   {}".format(item) for item in unmatched[:40])
    if len(unmatched) > 40:
        out.append("--   … {} more".format(len(unmatched) - 40))
    out.append("-- revisits skipped ({}):".format(len(dropped)))
    out.extend("--   {}".format(item) for item in dropped[:40])
    if len(dropped) > 40:
        out.append("--   … {} more".format(len(dropped) - 40))
    out.append("")

    out.extend(
        [
            "BEGIN;",
            "",
            "-- schema (same DDL as the V45/V48 migrations, idempotent)",
            "CREATE TABLE IF NOT EXISTS bus_lines",
            "(",
            "    id         BIGINT GENERATED BY DEFAULT AS IDENTITY NOT NULL,",
            "    number     VARCHAR(255)                            NOT NULL,",
            "    name       VARCHAR(255)                            NOT NULL,",
            "    direction  VARCHAR(255)                            NOT NULL,",
            "    origin     VARCHAR(255)                            NOT NULL,",
            "    terminus   VARCHAR(255)                            NOT NULL,",
            "    color      VARCHAR(255)                            NOT NULL,",
            "    stop_count INT                                    NOT NULL,",
            "    synced_at  TIMESTAMPTZ                            NOT NULL DEFAULT now(),",
            "    CONSTRAINT pk_bus_lines PRIMARY KEY (id),",
            "    CONSTRAINT uc_bus_lines_variant UNIQUE (name, direction, origin, terminus)",
            ");",
            "",
            "CREATE INDEX IF NOT EXISTS bus_lines_number_idx ON bus_lines (number);",
            "",
            "CREATE TABLE IF NOT EXISTS bus_line_stops",
            "(",
            "    line_id    BIGINT NOT NULL REFERENCES bus_lines (id) ON DELETE CASCADE,",
            "    stop_id    BIGINT NOT NULL REFERENCES stops (id) ON DELETE CASCADE,",
            "    stop_order INT,",
            "    CONSTRAINT pk_bus_line_stops PRIMARY KEY (line_id, stop_id)",
            ");",
            "",
            "CREATE INDEX IF NOT EXISTS bus_line_stops_stop_idx ON bus_line_stops (stop_id);",
            "",
            "ALTER TABLE bus_line_stops ADD COLUMN IF NOT EXISTS stop_order INT;",
            "",
            "CREATE UNIQUE INDEX IF NOT EXISTS bus_line_stops_order_uc",
            "    ON bus_line_stops (line_id, stop_order);",
            "",
        ]
    )

    for i in range(0, len(catalogue), 100):
        chunk = catalogue[i : i + 100]
        out.append("-- catalogue {}-{} ({} rows)".format(i + 1, i + len(chunk), len(chunk)))
        out.append("INSERT INTO bus_lines (number, name, direction, origin, terminus, color, stop_count)")
        out.append("VALUES")
        out.append(",\n".join(chunk))
        out.append("ON CONFLICT (name, direction, origin, terminus) DO UPDATE")
        out.append("   SET number = excluded.number,")
        out.append("       color = excluded.color,")
        out.append("       stop_count = excluded.stop_count,")
        out.append("       synced_at = now();")
        out.append("")

    out.append("-- only the variants re-imported here lose their links")
    out.append("DELETE FROM bus_line_stops")
    out.append(" WHERE line_id IN (")
    out.append("    SELECT l.id FROM bus_lines l")
    out.append("      JOIN (VALUES")
    out.append(",\n".join("  " + v for v in ordered_variants))
    out.append("      ) AS r(line_name, direction, origin, terminus)")
    out.append("        ON l.name = r.line_name AND l.direction = r.direction")
    out.append("       AND l.origin = r.origin AND l.terminus = r.terminus")
    out.append(" );")
    out.append("")

    for i in range(0, len(rows), 250):
        chunk = rows[i : i + 250]
        out.append("-- links {}-{} ({} rows)".format(i + 1, i + len(chunk), len(chunk)))
        out.append("INSERT INTO bus_line_stops (line_id, stop_id, stop_order)")
        out.append("SELECT l.id, s.id, v.stop_order")
        out.append("  FROM (VALUES")
        out.append(",\n".join("    " + row for row in chunk))
        out.append("  ) AS v(line_name, direction, origin, terminus, stop_order, stop_name, lat, lon)")
        out.append("  JOIN bus_lines l")
        out.append("    ON l.name = v.line_name AND l.direction = v.direction")
        out.append("   AND l.origin = v.origin AND l.terminus = v.terminus")
        out.append("  JOIN stops s")
        out.append("    ON s.name = v.stop_name AND s.latitude = v.lat AND s.longitude = v.lon")
        out.append("ON CONFLICT DO NOTHING;")
        out.append("")

    out.append("COMMIT;")
    out.append("")
    out.append("-- =============================== VERIFICATION ===============================")
    out.append("-- 1. links with no position (expect 0; otherwise see query 6)")
    out.append("SELECT count(*) AS unordered_links FROM bus_line_stops WHERE stop_order IS NULL;")
    out.append("")
    out.append("-- 2. sequences with a hole (expect 0 rows)")
    out.append("SELECT line_id, count(*) AS links, max(stop_order) AS last_order")
    out.append("  FROM bus_line_stops")
    out.append(" WHERE stop_order IS NOT NULL")
    out.append(" GROUP BY line_id")
    out.append("HAVING max(stop_order) <> count(*) - 1;")
    out.append("")
    out.append("-- 3. ordered links (expect {})".format(len(rows)))
    out.append("SELECT count(*) AS ordered_links FROM bus_line_stops WHERE stop_order IS NOT NULL;")
    out.append("")
    out.append("-- 4. variants with no stop at all (expect 0)")
    out.append("SELECT l.id, l.name, l.direction FROM bus_lines l")
    out.append("  LEFT JOIN bus_line_stops bls ON bls.line_id = l.id")
    out.append(" WHERE bls.line_id IS NULL;")
    out.append("")
    out.append("-- 5. stops per variant vs the provider's stop_count (review, not necessarily 0)")
    out.append("SELECT l.number, l.name, l.direction, l.stop_count, count(bls.stop_id) AS links")
    out.append("  FROM bus_lines l LEFT JOIN bus_line_stops bls ON bls.line_id = l.id")
    out.append(" GROUP BY l.id, l.number, l.name, l.direction, l.stop_count")
    out.append("HAVING count(bls.stop_id) <> l.stop_count")
    out.append(" ORDER BY l.number, l.name;")
    out.append("")
    out.append("-- 6. variants left without an ordered link (expect 0)")
    out.append("SELECT l.number, l.name, l.direction FROM bus_lines l")
    out.append("  LEFT JOIN bus_line_stops bls")
    out.append("    ON bls.line_id = l.id AND bls.stop_order IS NOT NULL")
    out.append(" WHERE bls.line_id IS NULL")
    out.append(" ORDER BY l.number, l.name;")
    out.append("-- ============================================================================")
    return "\n".join(out) + "\n"


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--outdir", default=os.path.dirname(os.path.abspath(__file__)))
    parser.add_argument("--date", default=dt.date.today().isoformat())
    parser.add_argument("--from-json", help="reuse an existing stops snapshot (no network calls)")
    parser.add_argument("--neighborhoods-json", help="districts snapshot when using --from-json")
    parser.add_argument("--lines-json", help="lines snapshot when using --from-json")
    parser.add_argument(
        "--routes-json",
        help="ordered routes snapshot; required to emit ordered links offline",
    )
    parser.add_argument("--emit-sql", help="also write the stops INSERT script here")
    parser.add_argument(
        "--emit-lines-sql",
        help="also write the bus lines + ordered links INSERT script here",
    )
    args = parser.parse_args()

    if args.from_json:
        raw_stops = json.load(open(args.from_json, encoding="utf-8"))
        raw_stops = raw_stops["stops"] if isinstance(raw_stops, dict) else raw_stops
        nfile = args.neighborhoods_json or os.path.join(os.path.dirname(args.from_json), "q.json")
        raw_neighborhoods = json.load(open(nfile, encoding="utf-8"))
        if isinstance(raw_neighborhoods, dict):
            raw_neighborhoods = (
                raw_neighborhoods.get("neighborhoods")
                or raw_neighborhoods.get("quartiers")
                or raw_neighborhoods["valiny"]
            )
        else:
            raw_neighborhoods = raw_neighborhoods["valiny"]
        raw_lines = []
        if args.lines_json and os.path.exists(args.lines_json):
            loaded = json.load(open(args.lines_json, encoding="utf-8"))
            raw_lines = loaded["lines"] if isinstance(loaded, dict) else loaded
        raw_routes = []
        if args.routes_json and os.path.exists(args.routes_json):
            loaded = json.load(open(args.routes_json, encoding="utf-8"))
            raw_routes = loaded["routes"] if isinstance(loaded, dict) else loaded
        calls = 0
    else:
        base_url = os.environ.get("TAXIB_DATA_BASE_URL")
        api_key = os.environ.get("TAXIB_DATA_API_KEY")
        if not base_url or not api_key:
            raise SystemExit("TAXIB_DATA_BASE_URL and TAXIB_DATA_API_KEY must be set")
        client = DataClient(base_url, api_key)
        raw_neighborhoods = client.neighborhoods()
        stops = sweep_stops(client, raw_neighborhoods)
        fill_gaps(client, raw_neighborhoods, stops)
        raw_stops = list(stops.values())
        variants = sweep_lines(client, raw_neighborhoods)
        raw_lines = list(variants.values())
        raw_routes = sweep_routes(client, variants)
        calls = client.calls
        if client.errors:
            print("provider errors:", *client.errors[:5], sep="\n  ", file=sys.stderr)

    neighborhoods = [_neighborhood_record(n) for n in raw_neighborhoods]
    stops = [_stop_record(s) for s in raw_stops]
    lines = [_line_record(v) for v in raw_lines]
    # Offline routes are already normalized; network ones are not.
    routes = [
        route if isinstance(route.get("stops"), list) else {**route, "stops": []}
        for route in raw_routes
    ]

    expected = {n["name"]: n["stop_count"] for n in neighborhoods}
    per = Counter(s["neighborhood"] for s in stops)
    mismatches = [(n, e, per.get(n, 0)) for n, e in expected.items() if per.get(n, 0) != e]
    unknown = {k: v for k, v in per.items() if k not in expected}

    fetched_at = dt.datetime.now(dt.timezone.utc).replace(microsecond=0).isoformat()
    notes = (
        "bbox grid sweep with per-district gap fill; stop ids are '<slug>@<district>'; "
        "coordinates are WGS84 (latitude, longitude); per-district counts match the "
        "provider's expected counts ({}/{})".format(
            len(expected) - len(mismatches), len(expected)
        )
    )

    os.makedirs(args.outdir, exist_ok=True)
    written = []
    documents = [
        ("taxib-neighborhoods-{}.json", {
            "source": "taxi-be open data",
            "source_notes": "served districts with stop count, served lines and centre coordinates",
            "fetched_at": fetched_at,
            "count": len(neighborhoods),
            "neighborhoods": neighborhoods,
        }),
        ("taxib-stops-{}.json", {
            "source": "taxi-be open data",
            "source_notes": notes,
            "fetched_at": fetched_at,
            "count": len(stops),
            "stops": stops,
        }),
        ("taxib-lines-{}.json", {
            "source": "taxi-be open data",
            "source_notes": (
                "one entry per bus line variant (a line and a direction); "
                "geometry is not included, it is returned per route instead"
            ),
            "fetched_at": fetched_at,
            "count": len(lines),
            "lines": lines,
        }),
        ("taxib-routes-{}.json", {
            "source": "taxi-be open data",
            "source_notes": (
                "ordered stops per line variant, in route order, straight from the "
                "provider's route endpoint; a loop line keeps the first visit only"
            ),
            "fetched_at": fetched_at,
            "count": len(routes),
            "routes": routes,
        }),
    ]

    # Never overwrite a good routes snapshot with an empty one: offline runs that
    # do not pass --routes-json have nothing to say about order.
    if not routes:
        documents = [doc for doc in documents if not doc[0].startswith("taxib-routes")]

    for filename, doc in documents:
        path = os.path.join(args.outdir, filename.format(args.date))
        with open(path, "w", encoding="utf-8") as fh:
            json.dump(doc, fh, ensure_ascii=False, indent=2)
        written.append((path, doc["count"]))

    for path, count in written:
        print("{} -> {} items".format(path, count))
    if args.emit_sql:
        with open(args.emit_sql, "w", encoding="utf-8") as fh:
            fh.write(build_sql(stops, expected))
        print("sql -> {}".format(args.emit_sql))
    if args.emit_lines_sql:
        if not routes:
            raise SystemExit(
                "no ordered routes: pass --routes-json, or let the sweep fetch them "
                "(TAXIB_DATA_ROUTE_PATH points at the provider's route endpoint)"
            )
        with open(args.emit_lines_sql, "w", encoding="utf-8") as fh:
            fh.write(build_lines_sql(lines, routes, stops, args.date))
        print("lines sql -> {}".format(args.emit_lines_sql))
    print("requests: {}".format(calls))
    print("per-district mismatches: {}".format(mismatches if mismatches else "none"))
    print("unknown district names  : {}".format(unknown if unknown else "none"))
    return 0 if not mismatches and not unknown else 1


if __name__ == "__main__":
    sys.exit(main())
