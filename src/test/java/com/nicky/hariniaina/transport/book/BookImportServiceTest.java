package com.nicky.hariniaina.transport.book;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nicky.hariniaina.transport.book.BookImportService.ParsedLine;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BookImportServiceTest {

  private static final String SAMPLE =
      """
      {"routes": [
        {"line": 163, "operator": "MIRINDRA", "phone": "0320711383",
         "directions": [
           {"name": "aller", "stops": ["Ankadikely Ilafy", "Anosy"]},
           {"name": "retour", "stops": ["Anosy", "Ankadikely Ilafy"]}
         ]},
        {"line": "H", "operator": "X", "phone": null,
         "directions": [{"name": "A", "stops": ["Ambodivona", "  ", "H"]}]},
        {"operator": "NoRef", "directions": [{"name": "aller", "stops": ["X"]}]}
      ]}""";

  @Mock private BookLineRepository lines;
  @Mock private BookDirectionRepository directions;
  @Mock private BookStopRepository stops;

  private BookImportService service() {
    return new BookImportService(lines, directions, stops, new ObjectMapper());
  }

  @Test
  void parsesLinesSkipsBlankStopsAndRefLess() throws Exception {
    List<ParsedLine> parsed = service().parseRoutes(SAMPLE);
    assertEquals(2, parsed.size());
    assertEquals("163", parsed.get(0).ref());
    assertEquals("MIRINDRA", parsed.get(0).operateur());
    assertEquals(2, parsed.get(0).directions().size());
    assertEquals("H", parsed.get(1).ref());
    assertEquals(List.of("Ambodivona", "H"), parsed.get(1).directions().get(0).stops());
  }

  @Test
  void importUpsertsDirectionsAndStops() {
    BookLine existing = new BookLine();
    existing.setId(7L);
    existing.setRef("163");
    when(lines.findByRef("163")).thenReturn(Optional.of(existing));

    long[] counts =
        service()
            .importRoutes(
                List.of(
                    new ParsedLine(
                        "163",
                        "MIRINDRA2",
                        "000",
                        List.of(
                            new BookImportService.ParsedDirection("aller", List.of("A", "B"))))));

    assertEquals(1, counts[0]);
    assertEquals(1, counts[1]);
    assertEquals(2, counts[2]);
    assertEquals("MIRINDRA2", existing.getOperateur());
    var dirCaptor = ArgumentCaptor.forClass(BookDirection.class);
    verify(directions).save(dirCaptor.capture());
    assertEquals("aller", dirCaptor.getValue().getSens());
    var stopCaptor = ArgumentCaptor.forClass(List.class);
    verify(stops).saveAll(stopCaptor.capture());
    List<BookStop> saved = stopCaptor.getValue().stream().map(o -> (BookStop) o).toList();
    assertEquals(List.of(1, 2), saved.stream().map(BookStop::getSeq).toList());
    verify(lines, org.mockito.Mockito.never()).save(any(BookLine.class));
  }
}
