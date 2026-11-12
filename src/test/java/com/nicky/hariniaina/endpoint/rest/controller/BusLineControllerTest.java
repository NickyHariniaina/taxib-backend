package com.nicky.hariniaina.endpoint.rest.controller;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.nicky.hariniaina.dto.BusLineResponse;
import com.nicky.hariniaina.dto.StopResponse;
import com.nicky.hariniaina.service.BusLineService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class BusLineControllerTest {

  private final BusLineService service = mock(BusLineService.class);

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.standaloneSetup(new BusLineController(service)).build();
  }

  @Test
  void shouldReturnLines() throws Exception {
    when(service.list(null, null, null, null, 500, 50))
        .thenReturn(
            List.of(
                new BusLineResponse(
                    "109", "109 67Ha", "Aller", "Ambohitromby", "Ny Havana", "#091f46", 48, null)));

    mockMvc
        .perform(get("/api/lines"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].number").value("109"))
        .andExpect(jsonPath("$[0].name").value("109 67Ha"))
        .andExpect(jsonPath("$[0].stopCount").value(48));
  }

  @Test
  void shouldReturnStopsOfBus() throws Exception {
    when(service.stopsOf("109", null, null, 100))
        .thenReturn(List.of(new StopResponse(1L, "Pharmacie", "Analakely", -18.9, 47.5, 12L)));

    mockMvc
        .perform(get("/api/lines/{number}/stops", "109"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].name").value("Pharmacie"))
        .andExpect(jsonPath("$[0].distanceM").value(12));
  }
}
