package com.nicky.hariniaina.transport.web;

import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.nicky.hariniaina.transport.service.NearbyStopService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(StopsController.class)
class StopsControllerTest {

  @Autowired private MockMvc mvc;
  @MockBean private NearbyStopService nearby;

  @Test
  void returnsStops() throws Exception {
    when(nearby.nearby(anyDouble(), anyDouble(), anyDouble(), anyInt()))
        .thenReturn(List.of(new StopDto(1, "Analakely", -18.91, 47.528, 120)));
    mvc.perform(get("/stops/nearby").param("lat", "-18.91").param("lon", "47.528"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].name").value("Analakely"))
        .andExpect(jsonPath("$[0].distanceM").value(120));
  }

  @Test
  void rejectsOutOfRangeLat() throws Exception {
    mvc.perform(get("/stops/nearby").param("lat", "95").param("lon", "47.5"))
        .andExpect(status().isBadRequest());
  }
}
