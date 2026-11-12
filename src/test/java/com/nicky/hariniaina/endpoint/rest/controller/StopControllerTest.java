package com.nicky.hariniaina.endpoint.rest.controller;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.nicky.hariniaina.dto.StopResponse;
import com.nicky.hariniaina.service.StopService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class StopControllerTest {

  private final StopService service = mock(StopService.class);

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.standaloneSetup(new StopController(service)).build();
  }

  @Test
  void shouldReturnNearbyStops() throws Exception {
    when(service.findNearby(-18.9, 47.5, 500, 10))
        .thenReturn(List.of(new StopResponse(1L, "Pharmacie", "Analakely", -18.9, 47.5, 12L)));

    mockMvc
        .perform(get("/api/stops").param("lat", "-18.9").param("lon", "47.5"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].name").value("Pharmacie"))
        .andExpect(jsonPath("$[0].neighborhood").value("Analakely"))
        .andExpect(jsonPath("$[0].distanceM").value(12));

    verify(service).findNearby(-18.9, 47.5, 500, 10);
  }
}
