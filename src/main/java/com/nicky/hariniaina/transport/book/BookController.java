package com.nicky.hariniaina.transport.book;

import com.nicky.hariniaina.transport.book.model.BookLineDetailDto;
import com.nicky.hariniaina.transport.book.model.BookLineDto;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/book")
public class BookController {

  private final BookService book;

  public BookController(BookService book) {
    this.book = book;
  }

  /** All lines for the line picker. */
  @GetMapping("/lines")
  public List<BookLineDto> lines() {
    return book.listLines();
  }

  /** Ordered stops per direction, the raw material for drawing a line on the map. */
  @GetMapping("/lines/{ref}")
  public BookLineDetailDto line(@PathVariable String ref) {
    return book.getLineDetail(ref);
  }
}
