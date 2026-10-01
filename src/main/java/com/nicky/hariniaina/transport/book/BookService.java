package com.nicky.hariniaina.transport.book;

import com.nicky.hariniaina.transport.book.model.BookDirectionDto;
import com.nicky.hariniaina.transport.book.model.BookLineDetailDto;
import com.nicky.hariniaina.transport.book.model.BookLineDto;
import com.nicky.hariniaina.transport.book.model.BookStopDto;
import java.util.Comparator;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Read-only access to the TaxiBoky reference data (ATT 2013). */
@Service
@Transactional(readOnly = true)
public class BookService {

  private final BookLineRepository lines;
  private final BookDirectionRepository directions;
  private final BookStopRepository stops;

  public BookService(
      BookLineRepository lines, BookDirectionRepository directions, BookStopRepository stops) {
    this.lines = lines;
    this.directions = directions;
    this.stops = stops;
  }

  /** All lines, refs sorted numerically (15 before 104). */
  public List<BookLineDto> listLines() {
    return lines.findAll().stream()
        .sorted(Comparator.comparing(BookLine::getRef, BookService::compareRefs))
        .map(l -> new BookLineDto(l.getRef(), l.getOperateur(), l.getTel()))
        .toList();
  }

  /** Full line detail with ordered stops per direction. 404 when the ref is unknown. */
  public BookLineDetailDto getLineDetail(String ref) {
    BookLine line =
        lines
            .findByRef(ref)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown line " + ref));
    List<BookDirectionDto> dirs =
        directions.findByLineId(line.getId()).stream()
            .map(
                d ->
                    new BookDirectionDto(
                        d.getSens(),
                        d.getNom(),
                        stops.findByDirectionIdOrderBySeqAsc(d.getId()).stream()
                            .map(s -> new BookStopDto(s.getSeq(), s.getNom()))
                            .toList()))
            .toList();
    return new BookLineDetailDto(line.getRef(), line.getOperateur(), line.getTel(), dirs);
  }

  private static int compareRefs(String a, String b) {
    try {
      return Integer.compare(Integer.parseInt(a), Integer.parseInt(b));
    } catch (NumberFormatException e) {
      return a.compareTo(b);
    }
  }
}
