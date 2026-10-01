package com.nicky.hariniaina.transport.book;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.nicky.hariniaina.transport.book.model.BookDirectionDto;
import com.nicky.hariniaina.transport.book.model.BookLineDetailDto;
import com.nicky.hariniaina.transport.book.model.BookLineDto;
import com.nicky.hariniaina.transport.book.model.BookStopDto;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

@WebMvcTest(BookController.class)
class BookControllerTest {

  @Autowired private MockMvc mvc;
  @MockBean private BookService book;

  @Test
  void returnsLines() throws Exception {
    when(book.listLines())
        .thenReturn(List.of(new BookLineDto("163", "TRANSPORT EXPRESS", "034 00 000 00")));
    mvc.perform(get("/book/lines"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].ref").value("163"))
        .andExpect(jsonPath("$[0].operateur").value("TRANSPORT EXPRESS"));
  }

  @Test
  void returnsLineDetail() throws Exception {
    when(book.getLineDetail("163"))
        .thenReturn(
            new BookLineDetailDto(
                "163",
                "TRANSPORT EXPRESS",
                "034 00 000 00",
                List.of(
                    new BookDirectionDto(
                        "A", "Ligne 163 - A", List.of(new BookStopDto(1, "Ankadikely Ilafy"))))));
    mvc.perform(get("/book/lines/163"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.ref").value("163"))
        .andExpect(jsonPath("$.directions[0].sens").value("A"))
        .andExpect(jsonPath("$.directions[0].stops[0].nom").value("Ankadikely Ilafy"));
  }

  @Test
  void unknownLineIs404() throws Exception {
    when(book.getLineDetail("999"))
        .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown line 999"));
    mvc.perform(get("/book/lines/999")).andExpect(status().isNotFound());
  }
}
