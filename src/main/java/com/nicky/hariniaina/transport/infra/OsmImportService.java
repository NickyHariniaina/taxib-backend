package com.nicky.hariniaina.transport.infra;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nicky.hariniaina.transport.geo.GeoMath;
import com.nicky.hariniaina.transport.model.Stop;
import com.nicky.hariniaina.transport.repo.StopRepository;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Pure-ish import logic: parse Overpass JSON, upsert stops. Network lives in the runner. */
@Service
@Slf4j
public class OsmImportService {

  /** One stop candidate parsed from an Overpass element. */
  public record ParsedStop(String osmId, String name, double lat, double lon) {}

  private final StopRepository stops;
  private final ObjectMapper mapper;

  public OsmImportService(StopRepository stops, ObjectMapper mapper) {
    this.stops = stops;
    this.mapper = mapper;
  }

  /** Extracts stop candidates from an Overpass `[out:json]` response. */
  public List<ParsedStop> parseStops(String overpassJson) throws Exception {
    List<ParsedStop> out = new ArrayList<>();
    JsonNode elements = mapper.readTree(overpassJson).path("elements");
    for (JsonNode el : elements) {
      if (!el.hasNonNull("lat") || !el.hasNonNull("lon")) {
        continue;
      }
      String osmId = el.path("type").asText("node") + "/" + el.path("id").asLong();
      JsonNode tags = el.path("tags");
      String name = tags.path("name").asText(null);
      if (name == null || name.isBlank()) {
        name = tags.path("ref").asText(null);
      }
      if (name == null || name.isBlank()) {
        name = "Unnamed stop";
      }
      out.add(new ParsedStop(osmId, name, el.path("lat").asDouble(), el.path("lon").asDouble()));
    }
    return out;
  }

  /** Inserts new stops, refreshes name/position of known ones. Returns inserted count. */
  @Transactional
  public int importStops(List<ParsedStop> parsed) {
    int inserted = 0;
    for (ParsedStop p : parsed) {
      Stop existing = stops.findByOsmId(p.osmId()).orElse(null);
      if (existing == null) {
        Stop s = new Stop();
        s.setOsmId(p.osmId());
        s.setName(p.name());
        s.setGeom(GeoMath.point(p.lat(), p.lon()));
        stops.save(s);
        inserted++;
      } else {
        existing.setName(p.name());
        existing.setGeom(GeoMath.point(p.lat(), p.lon()));
      }
    }
    log.info(
        "Import: {} parsed, {} inserted, {} already known",
        parsed.size(),
        inserted,
        parsed.size() - inserted);
    return inserted;
  }
}
