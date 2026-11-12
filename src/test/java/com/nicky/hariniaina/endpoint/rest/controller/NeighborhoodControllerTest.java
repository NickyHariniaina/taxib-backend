package com.nicky.hariniaina.endpoint.rest.controller;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.nicky.hariniaina.dto.NeighborhoodStopResponse;
import com.nicky.hariniaina.service.NeighborhoodService;
import com.nicky.hariniaina.service.StopService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class NeighborhoodControllerTest {

  private final NeighborhoodService neighborhoodService = mock(NeighborhoodService.class);
  private final StopService stopService = mock(StopService.class);

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    mockMvc =
        MockMvcBuilders.standaloneSetup(
                new NeighborhoodController(neighborhoodService, stopService))
            .build();
  }

  @Test
  void shouldReturnStopsOfNeighborhood() throws Exception {
    when(stopService.findByNeighborhood("Analakely"))
        .thenReturn(List.of(new NeighborhoodStopResponse(1L, "Pharmacie", -18.9, 47.5)));

    mockMvc
        .perform(get("/api/neighborhoods/Analakely/stops"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].name").value("Pharmacie"))
        .andExpect(jsonPath("$[0].id").value(1));
  }

  @Test
  void shouldSupportNamesWithSpaces() throws Exception {
    when(stopService.findByNeighborhood("67Ha Est"))
        .thenReturn(List.of(new NeighborhoodStopResponse(2L, "Terminus", -18.9, 47.5)));

    mockMvc
        .perform(get("/api/neighborhoods/{name}/stops", "67Ha Est"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].name").value("Terminus"));
  }
}
