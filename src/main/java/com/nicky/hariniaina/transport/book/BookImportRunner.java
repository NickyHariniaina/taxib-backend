package com.nicky.hariniaina.transport.book;

import java.nio.file.Files;
import java.nio.file.Path;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * One-shot TaxiBoky JSON import. Runs only with {@code --spring.profiles.active=import-book}, then
 * exits:
 *
 * <pre>
 *   export SPRING_DATASOURCE_URL=... SPRING_DATASOURCE_USERNAME=... SPRING_DATASOURCE_PASSWORD=...
 *   ./gradlew bootRun --args='--spring.profiles.active=import-book --taxib.import.book=/path/to/taxib-data.json'
 * </pre>
 */
@Component
@Profile("import-book")
@Slf4j
public class BookImportRunner implements ApplicationRunner {

  private final BookImportService importer;
  private final ApplicationContext context;
  private final String bookFile;

  public BookImportRunner(
      BookImportService importer,
      ApplicationContext context,
      @Value("${taxib.import.book:}") String bookFile) {
    this.importer = importer;
    this.context = context;
    this.bookFile = bookFile;
  }

  @Override
  public void run(ApplicationArguments args) {
    int exit = 0;
    try {
      if (bookFile.isBlank()) {
        throw new IllegalArgumentException("Missing --taxib.import.book=/path/to/taxib-data.json");
      }
      String json = Files.readString(Path.of(bookFile));
      var parsed = importer.parseRoutes(json);
      long[] counts = importer.importRoutes(parsed);
      log.info("Done: {} lines, {} directions, {} stops", counts[0], counts[1], counts[2]);
    } catch (Exception e) {
      log.error("Book import failed", e);
      exit = 1;
    }
    int code = exit;
    exit = SpringApplication.exit(context, () -> code);
    System.exit(exit);
  }
}
