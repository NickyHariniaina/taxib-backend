package com.nicky.hariniaina.transport.infra;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Locale;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * One-shot OSM import. Runs only with {@code --spring.profiles.active=import}, then exits:
 *
 * <pre>
 *   export SPRING_DATASOURCE_URL=... SPRING_DATASOURCE_USERNAME=... SPRING_DATASOURCE_PASSWORD=...
 *   ./gradlew bootRun --args='--spring.profiles.active=import --taxib.import.bbox=-18.89,47.51,-18.85,47.55'
 * </pre>
 *
 * Bbox format is south,west,north,east. Small cells: public Overpass instances throttle whole-city
 * queries (learned the hard way).
 */
@Component
@Profile("import")
@Slf4j
public class OsmImportRunner implements ApplicationRunner {

  private final OsmImportService importer;
  private final ApplicationContext context;
  private final String overpassUrl;
  private final String bbox;

  public OsmImportRunner(
      OsmImportService importer,
      ApplicationContext context,
      @Value("${taxib.import.overpass-url:https://overpass.kumi.systems/api/interpreter}")
          String overpassUrl,
      @Value("${taxib.import.bbox:-18.89,47.51,-18.85,47.55}") String bbox) {
    this.importer = importer;
    this.context = context;
    this.overpassUrl = overpassUrl;
    this.bbox = bbox;
  }

  @Override
  public void run(ApplicationArguments args) {
    int exit = 0;
    try {
      String[] box = bbox.split(",");
      double south = Double.parseDouble(box[0]);
      double west = Double.parseDouble(box[1]);
      double north = Double.parseDouble(box[2]);
      double east = Double.parseDouble(box[3]);
      // Locale.ROOT: %f must render dots (OSM), never locale commas.
      String ql =
          String.format(
              Locale.ROOT,
              "[out:json][timeout:120];(node[\"highway\"=\"bus_stop\"](%f,%f,%f,%f);"
                  + "node[\"public_transport\"=\"platform\"](%f,%f,%f,%f););out tags;",
              south,
              west,
              north,
              east,
              south,
              west,
              north,
              east);
      log.info("Fetching OSM stops in bbox {}", bbox);
      String body = "data=" + URLEncoder.encode(ql, StandardCharsets.UTF_8);
      HttpRequest request =
          HttpRequest.newBuilder(URI.create(overpassUrl))
              .header("Content-Type", "application/x-www-form-urlencoded")
              .POST(HttpRequest.BodyPublishers.ofString(body))
              .timeout(Duration.ofSeconds(150))
              .build();
      HttpResponse<String> response =
          HttpClient.newBuilder()
              .connectTimeout(Duration.ofSeconds(15))
              .build()
              .send(request, HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() != 200) {
        throw new IllegalStateException("Overpass failed: HTTP " + response.statusCode());
      }
      var parsed = importer.parseStops(response.body());
      int inserted = importer.importStops(parsed);
      log.info("Done: {} stops parsed, {} inserted", parsed.size(), inserted);
    } catch (Exception e) {
      log.error("Import failed", e);
      exit = 1;
    }
    int code = exit;
    exit = SpringApplication.exit(context, () -> code);
    System.exit(exit);
  }
}
