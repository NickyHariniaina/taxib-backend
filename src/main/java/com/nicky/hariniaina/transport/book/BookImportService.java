package com.nicky.hariniaina.transport.book;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Imports the transcribed TaxiBoky JSON (taxib-data.json) into book_* tables. Idempotent per line
 * ref: re-running refreshes a line instead of duplicating it.
 */
@Service
@Slf4j
public class BookImportService {

  /** One direction as listed in the JSON (name + ordered stop names). */
  public record ParsedDirection(String sens, List<String> stops) {}

  /** One line as listed in the JSON. */
  public record ParsedLine(
      String ref, String operateur, String tel, List<ParsedDirection> directions) {}

  private final BookLineRepository lines;
  private final BookDirectionRepository directions;
  private final BookStopRepository stops;
  private final ObjectMapper mapper;

  public BookImportService(
      BookLineRepository lines,
      BookDirectionRepository directions,
      BookStopRepository stops,
      ObjectMapper mapper) {
    this.lines = lines;
    this.directions = directions;
    this.stops = stops;
    this.mapper = mapper;
  }

  /** Parses the routes array of taxib-data.json. Layouts need no special case. */
  public List<ParsedLine> parseRoutes(String json) throws Exception {
    List<ParsedLine> out = new ArrayList<>();
    JsonNode routes = mapper.readTree(json).path("routes");
    for (JsonNode r : routes) {
      String ref = r.path("line").asText(null);
      if (ref == null || ref.isBlank()) {
        continue;
      }
      List<ParsedDirection> dirs = new ArrayList<>();
      for (JsonNode d : r.path("directions")) {
        List<String> names = new ArrayList<>();
        for (JsonNode s : d.path("stops")) {
          String name = s.asText(null);
          if (name != null && !name.isBlank()) {
            names.add(name.strip());
          }
        }
        if (!names.isEmpty()) {
          dirs.add(new ParsedDirection(d.path("name").asText(""), names));
        }
      }
      if (!dirs.isEmpty()) {
        out.add(
            new ParsedLine(
                ref.strip(), r.path("operator").asText(null), r.path("phone").asText(null), dirs));
      }
    }
    return out;
  }

  /** Upserts parsed lines. Returns [lines, directions, stops] counts. */
  @Transactional
  public long[] importRoutes(List<ParsedLine> parsed) {
    long dirCount = 0;
    long stopCount = 0;
    for (ParsedLine p : parsed) {
      BookLine line =
          lines
              .findByRef(p.ref())
              .map(
                  existing -> {
                    existing.setOperateur(p.operateur());
                    existing.setTel(p.tel());
                    return existing;
                  })
              .orElseGet(
                  () -> {
                    BookLine created = new BookLine();
                    created.setRef(p.ref());
                    created.setOperateur(p.operateur());
                    created.setTel(p.tel());
                    return lines.save(created);
                  });
      // Refresh directions wholesale: delete + recreate keeps seq/order exact.
      for (BookDirection old : directions.findByLineId(line.getId())) {
        directions.delete(old);
      }
      int seq;
      for (ParsedDirection d : p.directions()) {
        BookDirection direction = new BookDirection();
        direction.setLine(line);
        direction.setNom(d.sens().isBlank() ? p.ref() : d.sens());
        direction.setSens(d.sens().isBlank() ? p.ref() : d.sens());
        direction = directions.save(direction);
        dirCount++;
        seq = 0;
        List<BookStop> batch = new ArrayList<>();
        for (String name : d.stops()) {
          BookStop s = new BookStop();
          s.setDirection(direction);
          s.setSeq(++seq);
          s.setNom(name);
          batch.add(s);
        }
        stops.saveAll(batch);
        stopCount += batch.size();
      }
    }
    log.info("Book import: {} lines, {} directions, {} stops", parsed.size(), dirCount, stopCount);
    return new long[] {parsed.size(), dirCount, stopCount};
  }
}
